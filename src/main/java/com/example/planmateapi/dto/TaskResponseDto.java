package com.example.planmateapi.dto;

import com.example.planmateapi.entity.RepeatType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponseDto {
    private Long id;
    private String title;
    private String description;
    private Set<TagDto.Response> tags;

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
}