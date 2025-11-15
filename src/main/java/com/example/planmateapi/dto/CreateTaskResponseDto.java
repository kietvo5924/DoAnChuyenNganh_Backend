package com.example.planmateapi.dto;

import java.time.OffsetDateTime;

// Dùng record cho ngắn gọn, hoặc bạn có thể dùng @Data @Builder
public record CreateTaskResponseDto(
        boolean success,
        String message,
        String taskTitle,
        OffsetDateTime startTime,
        String calendarName
) {
}