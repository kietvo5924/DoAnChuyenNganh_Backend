package com.example.planmateapi.controller;

import com.example.planmateapi.dto.ChatRequest;
import com.example.planmateapi.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "Chatbot API")
@SecurityRequirement(name = "bearerAuth")
public class ChatbotController {

    private final ChatService chatService;

    @PostMapping
    @Operation(summary = "Gửi tin nhắn đến Chatbot")
    public ResponseEntity<String> handleChat(@RequestBody ChatRequest request) {
        String response = chatService.chat(request);
        return ResponseEntity.ok(response);
    }
}
