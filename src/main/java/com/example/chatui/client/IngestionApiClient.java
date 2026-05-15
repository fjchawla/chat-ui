package com.example.chatui.client;

import com.example.chatui.config.FeignConfig;
import com.example.chatui.model.IngestionStatus;
import com.example.chatui.model.MrrEvalRequest;
import com.example.chatui.model.MrrEvalResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@FeignClient(name = "ingestion-api", url = "${backend.base-url}", configuration = FeignConfig.class)
public interface IngestionApiClient {

    /** POST /api/ingest/upload — upload a PDF and immediately ingest it. */
    @PostMapping(value = "/api/ingest/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    IngestionStatus upload(@RequestPart("file") MultipartFile file);

    /** POST /api/ingest/trigger — ingest a PDF already present on the server by filename. */
    @PostMapping("/api/ingest/trigger")
    IngestionStatus triggerExisting(@RequestParam("filename") String filename);

    /** POST /api/ingest/trigger-all — ingest every PDF in the server's upload directory. */
    @PostMapping("/api/ingest/trigger-all")
    List<IngestionStatus> triggerAll();

    /** GET /api/ingest/status — list completed/running ingestion jobs. */
    @GetMapping("/api/ingest/status")
    List<Object> status();

    /** POST /api/ingest/evaluate — run MRR evaluation against test queries. */
    @PostMapping("/api/ingest/evaluate")
    MrrEvalResult evaluate(@RequestBody MrrEvalRequest request);
}
