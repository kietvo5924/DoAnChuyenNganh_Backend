package com.example.planmateapi.controller;

import com.example.planmateapi.dto.*;
import com.example.planmateapi.service.CalendarShareService;
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
@Tag(name = "Calendar Sharing API")
@SecurityRequirement(name = "bearerAuth")
public class CalendarShareController {

    private final CalendarShareService calendarShareService;

    @PostMapping("/{calendarId}/share")
    public ResponseEntity<String> shareCalendar(
            @PathVariable Long calendarId,
            @Valid @RequestBody ShareRequestDto request) {
        calendarShareService.shareCalendar(calendarId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body("Chia sẻ lịch thành công.");
    }

    @DeleteMapping("/{calendarId}/unshare/{userId}")
    public ResponseEntity<Void> unshareCalendar(
            @PathVariable Long calendarId,
            @PathVariable Long userId) {
        calendarShareService.unshareCalendar(calendarId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{calendarId}/users")
    public ResponseEntity<List<UserResponseDTO>> getUsersSharingCalendar(@PathVariable Long calendarId) {
        List<UserResponseDTO> users = calendarShareService.getUsersSharingCalendar(calendarId);
        return ResponseEntity.ok(users);
    }

    // Lấy danh sách lịch được chia sẻ VỚI TÔI
    @GetMapping("/shared-with-me")
    public ResponseEntity<List<SharedCalendarResponseDto>> getCalendarsSharedWithMe() {
        List<SharedCalendarResponseDto> calendars = calendarShareService.getCalendarsSharedWithMe();
        return ResponseEntity.ok(calendars);
    }
}