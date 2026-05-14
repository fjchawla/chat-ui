package com.example.chatui.client;

import com.example.chatui.model.ChatRequest;
import com.example.chatui.model.ChatResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "chat-api", url = "${backend.base-url}")
public interface ChatApiClient {

    @PostMapping("/api/chat")
    ChatResponse chat(@RequestBody ChatRequest request);
}
