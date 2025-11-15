package com.example.planmateapi.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

// Dùng tên khác với record trong AiConfig để tránh nhầm lẫn
public record AiCreateTaskRequest(
        @JsonPropertyDescription("Tiêu đề của công việc hoặc lịch hẹn")
        String title,
        @JsonPropertyDescription("Thời gian bắt đầu ISO 8601")
        String startTime,
        @JsonPropertyDescription("Thời gian kết thúc ISO 8601 (Nếu không có, AI tự tính +1 giờ)")
        String endTime
) {
}