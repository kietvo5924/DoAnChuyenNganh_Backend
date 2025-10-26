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

    // --- QUẢN LÝ VIỆC SHARE (CHỈ CHỦ LỊCH) ---

    @PostMapping("/calendars/{calendarId}")
    public ResponseEntity<String> shareCalendar(
            @PathVariable Long calendarId,
            @Valid @RequestBody ShareRequestDto request) {
        calendarShareService.shareCalendar(calendarId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body("Chia sẻ lịch thành công.");
    }

    @DeleteMapping("/calendars/{calendarId}/unshare/{userId}")
    public ResponseEntity<Void> unshareCalendar(
            @PathVariable Long calendarId,
            @PathVariable Long userId) {
        calendarShareService.unshareCalendar(calendarId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/calendars/{calendarId}/users")
    public ResponseEntity<List<UserResponseDTO>> getUsersSharingCalendar(@PathVariable Long calendarId) {
        List<UserResponseDTO> users = calendarShareService.getUsersSharingCalendar(calendarId);
        return ResponseEntity.ok(users);
    }

    // --- API CHO NGƯỜI ĐƯỢC SHARE (ONLINE-ONLY) ---

    // Lấy danh sách lịch được chia sẻ VỚI TÔI
    @GetMapping("/calendars/shared-with-me")
    public ResponseEntity<List<CalendarDto.Response>> getCalendarsSharedWithMe() {
        List<CalendarDto.Response> calendars = calendarShareService.getCalendarsSharedWithMe();
        return ResponseEntity.ok(calendars);
    }

    // Lấy chi tiết 1 lịch được chia sẻ
    @GetMapping("/calendars/{calendarId}")
    public ResponseEntity<CalendarDto.Response> getSharedCalendarById(@PathVariable Long calendarId) {
        CalendarDto.Response calendar = calendarShareService.getSharedCalendarById(calendarId);
        return ResponseEntity.ok(calendar);
    }

    // Sửa chi tiết 1 lịch được chia sẻ
    @PutMapping("/calendars/{calendarId}")
    public ResponseEntity<CalendarDto.Response> updateSharedCalendar(
            @PathVariable Long calendarId,
            @Valid @RequestBody CalendarDto.Request request) {
        CalendarDto.Response updated = calendarShareService.updateSharedCalendar(calendarId, request);
        return ResponseEntity.ok(updated);
    }

    // Lấy TẤT CẢ task trong 1 lịch được chia sẻ
    @GetMapping("/calendars/{calendarId}/tasks")
    public ResponseEntity<List<TaskResponseDto>> getAllTasksInSharedCalendar(@PathVariable Long calendarId) {
        List<TaskResponseDto> tasks = calendarShareService.getAllTasksInSharedCalendar(calendarId);
        return ResponseEntity.ok(tasks);
    }

    // Tạo task MỚI trong 1 lịch được chia sẻ
    @PostMapping("/calendars/{calendarId}/tasks")
    public ResponseEntity<Void> createTaskInSharedCalendar(
            @PathVariable Long calendarId,
            @Valid @RequestBody TaskRequestDto request) {
        calendarShareService.createOrUpdateTaskInSharedCalendar(calendarId, null, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // Cập nhật task trong 1 lịch được chia sẻ
    @PutMapping("/tasks/{taskId}")
    public ResponseEntity<Void> updateTaskInSharedCalendar(
            @PathVariable Long taskId,
            @RequestParam Long calendarId,
            @Valid @RequestBody TaskRequestDto request) {
        calendarShareService.createOrUpdateTaskInSharedCalendar(calendarId, taskId, request);
        return ResponseEntity.ok().build();
    }

    // Xóa task trong 1 lịch được chia sẻ
    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<Void> deleteTaskInSharedCalendar(
            @PathVariable Long taskId,
            @RequestParam String type) {
        calendarShareService.deleteTaskInSharedCalendar(taskId, type);
        return ResponseEntity.noContent().build();
    }
}
