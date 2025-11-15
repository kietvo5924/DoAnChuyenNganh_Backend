package com.example.planmateapi.dto;

// Dùng record cho đơn giản
public record MessageDto(
        String role,  // "user" hoặc "assistant"
        String content
) {}