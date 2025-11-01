package com.example.planmateapi.dto;

import com.example.planmateapi.entity.RepeatType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Set;

@Data
public class TaskRequestDto {

    @NotBlank
    private String title;
    private String description;
    private Set<Long> tagIds;

    @NotNull
    private RepeatType repeatType;

    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private boolean isAllDay;

    private LocalTime repeatStartTime;
    private LocalTime repeatEndTime;
    private String timezone;
    private Integer repeatInterval;
    private String repeatDays;
    private Integer repeatDayOfMonth;
    private Integer repeatWeekOfMonth;
    private Integer repeatDayOfWeek;
    private LocalDate repeatStart;
    private LocalDate repeatEnd;
    private String exceptions;
    private boolean preDayNotify;
}