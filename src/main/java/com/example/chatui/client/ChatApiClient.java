package com.example.chatui.client;

import com.example.chatui.model.ChatJobStatus;
import com.example.chatui.model.ChatRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "chat-api", url = "${backend.base-url}")
public interface ChatApiClient {

    /** POST /api/chat — submit query; backend returns 202 immediately with jobId. */
    @PostMapping("/api/chat")
    ChatJobStatus chat(@RequestBody ChatRequest request);

    /** GET /api/chat/result/{jobId} — 202 while processing, 200 when done, 500 on failure. */
    @GetMapping("/api/chat/result/{jobId}")
    Object chatResult(@PathVariable("jobId") String jobId);
}
