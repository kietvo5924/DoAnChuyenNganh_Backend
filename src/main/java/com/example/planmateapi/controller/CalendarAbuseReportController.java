package com.example.planmateapi.controller;

import com.example.planmateapi.dto.CalendarReportRequestDto;
import com.example.planmateapi.service.CalendarAbuseReportService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/calendar-reports")
@RequiredArgsConstructor
@Tag(name = "Calendar Abuse Reports")
@SecurityRequirement(name = "bearerAuth")
public class CalendarAbuseReportController {

    private final CalendarAbuseReportService calendarAbuseReportService;

    @PostMapping
    public ResponseEntity<String> reportCalendar(@Valid @RequestBody CalendarReportRequestDto requestDto) {
        calendarAbuseReportService.submitReport(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body("Đã gửi báo cáo đến Admin.");
    }
}
