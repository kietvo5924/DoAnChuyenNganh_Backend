package com.example.planmateapi.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Set;

public class TaskDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotBlank(message = "Tiêu đề không được để trống")
        private String title;
        private String description;

        @NotNull(message = "Thời gian bắt đầu không được để trống")
        private OffsetDateTime startTime;

        @NotNull(message = "Thời gian kết thúc không được để trống")
        private OffsetDateTime endTime;

        private boolean isAllDay = false;

        private Set<Long> tagIds;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String title;
        private String description;
        private OffsetDateTime startTime;
        private OffsetDateTime endTime;
        private boolean isAllDay;
        private Set<TagDto.Response> tags;
    }
}