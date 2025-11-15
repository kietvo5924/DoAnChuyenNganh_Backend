package com.example.planmateapi.dto;

import java.util.List;

// THAY THẾ record cũ bằng record này
public record ChatRequest(
        String message, // Tin nhắn mới nhất
        List<MessageDto> history // Lịch sử các tin nhắn trước đó
) {}