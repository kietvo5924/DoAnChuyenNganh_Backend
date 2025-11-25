package com.example.planmateapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CalendarReportRequestDto {

    @NotNull
    private Long calendarId;

    @NotBlank
    private String reason;

    private String description;
}
