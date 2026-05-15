package com.example.chatui.controller;

import com.example.chatui.client.ChatApiClient;
import com.example.chatui.client.IngestionApiClient;
import com.example.chatui.model.ChatJobStatus;
import com.example.chatui.model.ChatRequest;
import com.example.chatui.model.IngestionStatus;
import com.example.chatui.model.MrrEvalRequest;
import com.example.chatui.model.MrrEvalResult;
import feign.FeignException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import java.util.List;
import java.util.Map;

@Controller
public class WebController {

    private final ChatApiClient chatClient;
    private final IngestionApiClient ingestionClient;
    private final JdbcTemplate jdbc;

    public WebController(ChatApiClient chatClient, IngestionApiClient ingestionClient, JdbcTemplate jdbc) {
        this.chatClient = chatClient;
        this.ingestionClient = ingestionClient;
        this.jdbc = jdbc;
    }

    // ── Page routes ────────────────────────────────────────────────────

    @GetMapping("/")
    public String index() { return "index"; }

    @GetMapping("/login")
    public String login() { return "login"; }

    @GetMapping("/ingest/upload")
    public String ingestUpload() { return "ingest/upload"; }

    @GetMapping("/ingest/trigger")
    public String ingestTrigger() { return "ingest/trigger"; }

    @GetMapping("/ingest/status")
    public String ingestStatus() { return "ingest/status"; }

    @GetMapping("/ingest/evaluate")
    public String ingestEvaluate() { return "ingest/evaluate"; }

    @GetMapping("/demo")
    public String demo() { return "demo/index"; }

    // ── Chat API ───────────────────────────────────────────────────────

    @PostMapping("/api/chat")
    @ResponseBody
    public ResponseEntity<?> chat(@RequestBody ChatRequest request) {
        try {
            ChatJobStatus job = chatClient.chat(request);
            return ResponseEntity.status(202).body(job);
        } catch (FeignException.ServiceUnavailable | FeignException.InternalServerError e) {
            return ResponseEntity.status(502)
                .body(Map.of("error", "Backend service unavailable. Please try again."));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("error", "Unexpected error: " + e.getMessage()));
        }
    }

    @GetMapping("/api/chat/result/{jobId}")
    @ResponseBody
    public ResponseEntity<?> chatResult(@PathVariable String jobId) {
        try {
            Object result = chatClient.chatResult(jobId);
            // 202 body: {jobId, status:"PROCESSING"} — pass through with 202
            if (result instanceof Map<?, ?> map && "PROCESSING".equals(map.get("status"))) {
                return ResponseEntity.status(202).body(result);
            }
            return ResponseEntity.ok(result);
        } catch (FeignException.InternalServerError e) {
            return ResponseEntity.status(500)
                .body(Map.of("error", "Chat job failed: " + e.getMessage()));
        } catch (FeignException.ServiceUnavailable e) {
            return ResponseEntity.status(502)
                .body(Map.of("error", "Backend service unavailable. Please try again."));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("error", "Unexpected error: " + e.getMessage()));
        }
    }

    // ── Ingestion API ──────────────────────────────────────────────────

    @PostMapping("/api/upload")
    @ResponseBody
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No file selected."));
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".pdf")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only PDF files are accepted."));
        }
        try {
            IngestionStatus status = ingestionClient.upload(file);
            // Backend returns 202 Accepted when ingestion is async (status == PROCESSING)
            int httpStatus = "PROCESSING".equals(status.status()) ? 202 : 200;
            return ResponseEntity.status(httpStatus).body(status);
        } catch (FeignException.ServiceUnavailable e) {
            return ResponseEntity.status(502).body(Map.of("error", "Backend service unavailable."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Upload failed: " + e.getMessage()));
        }
    }

    @PostMapping("/api/ingest/trigger")
    @ResponseBody
    public ResponseEntity<?> triggerIngestion(@RequestParam("filename") String filename) {
        try {
            IngestionStatus status = ingestionClient.triggerExisting(filename);
            return ResponseEntity.ok(status);
        } catch (FeignException.NotFound e) {
            return ResponseEntity.status(404).body(Map.of("error", "File not found on server: " + filename));
        } catch (FeignException.ServiceUnavailable e) {
            return ResponseEntity.status(502).body(Map.of("error", "Backend service unavailable."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Trigger failed: " + e.getMessage()));
        }
    }

    @PostMapping("/api/ingest/trigger-all")
    @ResponseBody
    public ResponseEntity<?> triggerAll() {
        try {
            List<IngestionStatus> statuses = ingestionClient.triggerAll();
            return ResponseEntity.ok(statuses);
        } catch (FeignException.ServiceUnavailable e) {
            return ResponseEntity.status(502).body(Map.of("error", "Backend service unavailable."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Trigger-all failed: " + e.getMessage()));
        }
    }

    @PostMapping("/api/ingest/evaluate")
    @ResponseBody
    public ResponseEntity<?> evaluate(@RequestBody MrrEvalRequest request) {
        try {
            MrrEvalResult result = ingestionClient.evaluate(request);
            return ResponseEntity.ok(result);
        } catch (FeignException.ServiceUnavailable e) {
            return ResponseEntity.status(502).body(Map.of("error", "Backend service unavailable."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Evaluation failed: " + e.getMessage()));
        }
    }

    @GetMapping("/api/ingest/status")
    @ResponseBody
    public ResponseEntity<?> ingestionStatus() {
        try {
            List<IngestionStatus> rows = jdbc.query(
                "SELECT id, source_file, status, chunks_count, error_message " +
                "FROM (SELECT DISTINCT ON (source_file) id, source_file, status, chunks_count, error_message " +
                "      FROM ingestion_log ORDER BY source_file, id DESC) latest " +
                "ORDER BY id DESC",
                (rs, i) -> new IngestionStatus(
                    rs.getLong("id"),
                    rs.getString("source_file"),
                    rs.getString("status"),
                    rs.getObject("chunks_count", Integer.class),
                    rs.getString("error_message")
                )
            );
            return ResponseEntity.ok(rows);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Status fetch failed: " + e.getMessage()));
        }
    }

    @GetMapping("/api/ingest/status/{jobId}")
    @ResponseBody
    public ResponseEntity<?> ingestionStatusById(@PathVariable Long jobId) {
        try {
            Optional<IngestionStatus> row = jdbc.query(
                "SELECT id, source_file, status, chunks_count, error_message " +
                "FROM ingestion_log WHERE id = ?",
                (rs, i) -> new IngestionStatus(
                    rs.getLong("id"),
                    rs.getString("source_file"),
                    rs.getString("status"),
                    rs.getObject("chunks_count", Integer.class),
                    rs.getString("error_message")
                ),
                jobId
            ).stream().findFirst();
            return row.<ResponseEntity<?>>map(ResponseEntity::ok)
                      .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "Job not found.")));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Status fetch failed: " + e.getMessage()));
        }
    }
}
