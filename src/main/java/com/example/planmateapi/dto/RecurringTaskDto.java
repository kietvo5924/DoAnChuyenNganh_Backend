package com.example.planmateapi.dto;

import com.example.planmateapi.entity.RepeatType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

public class RecurringTaskDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotBlank(message = "Tiêu đề không được để trống")
        private String title;

        private String description;

        @NotNull(message = "Giờ bắt đầu không được để trống")
        private LocalTime startTime;

        @NotNull(message = "Giờ kết thúc không được để trống")
        private LocalTime endTime;

        private String timezone;

        @NotNull(message = "Kiểu lặp lại không được để trống")
        private RepeatType repeatType;

        private Integer repeatInterval;

        private String repeatDays;

        private Integer repeatDayOfMonth;

        private Integer repeatWeekOfMonth;

        private Integer repeatDayOfWeek;

        @NotNull(message = "Ngày bắt đầu lặp không được để trống")
        private LocalDate repeatStart;

        private LocalDate repeatEnd;

        private String exceptions;

        private Set<Long> tagIds;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String title;
        private String description;
        private LocalTime startTime;
        private LocalTime endTime;
        private String timezone;
        private RepeatType repeatType;
        private Integer repeatInterval;
        private String repeatDays;
        private Integer repeatDayOfMonth;
        private Integer repeatWeekOfMonth;
        private Integer repeatDayOfWeek;
        private LocalDate repeatStart;
        private LocalDate repeatEnd;
        private String exceptions;
        private Set<TagDto.Response> tags;
    }
}