package com.example.planmateapi.controller;

import com.example.planmateapi.dto.CalendarDto;
import com.example.planmateapi.service.CalendarService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/calendars")
@RequiredArgsConstructor
@Tag(name = "Calendar API")
@SecurityRequirement(name = "bearerAuth")
public class CalendarController {

    private final CalendarService calendarService;

    @PostMapping
    public ResponseEntity<CalendarDto.Response> createCalendar(@RequestBody @Valid CalendarDto.Request request) {
        CalendarDto.Response createdCalendar = calendarService.createCalendar(request);
        return new ResponseEntity<>(createdCalendar, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<CalendarDto.Response>> getAllCalendars() {
        List<CalendarDto.Response> calendars = calendarService.getAllCalendarsForCurrentUser();
        return ResponseEntity.ok(calendars);
    }

    // Endpoint để đặt lịch làm mặc định
    @PutMapping("/{calendarId}/set-default")
    public ResponseEntity<Void> setDefaultCalendar(@PathVariable Long calendarId) {
        calendarService.setDefaultCalendar(calendarId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{calendarId}")
    public ResponseEntity<CalendarDto.Response> updateCalendar(@PathVariable Long calendarId, @RequestBody @Valid CalendarDto.Request request) {
        CalendarDto.Response calendars = calendarService.updateCalendar(calendarId, request);
        return ResponseEntity.ok(calendars);
    }

    @DeleteMapping("/{calendarId}")
    public ResponseEntity<Void> deleteCalendar(@PathVariable Long calendarId) {
        calendarService.deleteCalendar(calendarId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{calendarId}")
    public ResponseEntity<CalendarDto.Response> getCalendarById(@PathVariable Long calendarId) {
        CalendarDto.Response calendar = calendarService.getCalendarById(calendarId);
        return ResponseEntity.ok(calendar);
    }
}
