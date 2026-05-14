package com.example.chatui.controller;

import com.example.chatui.client.ChatApiClient;
import com.example.chatui.client.IngestionApiClient;
import com.example.chatui.model.ChatRequest;
import com.example.chatui.model.ChatResponse;
import com.example.chatui.model.IngestionStatus;
import feign.FeignException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Controller
public class WebController {

    private final ChatApiClient chatClient;
    private final IngestionApiClient ingestionClient;

    public WebController(ChatApiClient chatClient, IngestionApiClient ingestionClient) {
        this.chatClient = chatClient;
        this.ingestionClient = ingestionClient;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

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
            return ResponseEntity.status(502)
                .body(Map.of("error", "Backend service unavailable."));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("error", "Upload failed: " + e.getMessage()));
        }
    }
}
