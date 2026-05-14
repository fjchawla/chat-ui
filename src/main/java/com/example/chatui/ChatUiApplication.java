package com.example.chatui;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class ChatUiApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChatUiApplication.class, args);
    }
}
