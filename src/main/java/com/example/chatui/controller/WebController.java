package com.example.chatui.controller;

import com.example.chatui.client.ChatApiClient;
import com.example.chatui.client.IngestionApiClient;
import com.example.chatui.model.ChatRequest;
import com.example.chatui.model.ChatResponse;
import com.example.chatui.model.IngestionStatus;
import com.example.chatui.model.MrrEvalRequest;
import com.example.chatui.model.MrrEvalResult;
import feign.FeignException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Controller
public class WebController {

    private final ChatApiClient chatClient;
    private final IngestionApiClient ingestionClient;

    public WebController(ChatApiClient chatClient, IngestionApiClient ingestionClient) {
        this.chatClient = chatClient;
        this.ingestionClient = ingestionClient;
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
            return ResponseEntity.ok(chatClient.chat(request));
        } catch (FeignException.ServiceUnavailable | FeignException.InternalServerError e) {
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
            return ResponseEntity.ok(status);
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
            List<Object> status = ingestionClient.status();
            return ResponseEntity.ok(status);
        } catch (FeignException.ServiceUnavailable e) {
            return ResponseEntity.status(502).body(Map.of("error", "Backend service unavailable."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Status fetch failed: " + e.getMessage()));
        }
    }
}
