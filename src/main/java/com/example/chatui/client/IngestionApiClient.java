package com.example.chatui.client;

import com.example.chatui.config.FeignConfig;
import com.example.chatui.model.IngestionStatus;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "ingestion-api", url = "${backend.base-url}", configuration = FeignConfig.class)
public interface IngestionApiClient {

    @PostMapping(value = "/api/ingest/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    IngestionStatus upload(@RequestPart("file") MultipartFile file);
}
