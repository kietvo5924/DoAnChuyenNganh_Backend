package com.example.planmateapi.controller;

import com.example.planmateapi.dto.RecurringTaskDto;
import com.example.planmateapi.service.RecurringTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Recurring Task API")
@SecurityRequirement(name = "bearerAuth")
public class RecurringTaskController {

    private final RecurringTaskService recurringTaskService;

    @Operation(summary = "Tạo một công việc lặp lại mới trong một lịch cụ thể")
    @PostMapping("/api/calendars/{calendarId}/recurring-tasks")
    public ResponseEntity<RecurringTaskDto.Response> createRecurringTask(
            @PathVariable Long calendarId,
            @Valid @RequestBody RecurringTaskDto.Request request) {
        RecurringTaskDto.Response createdTask = recurringTaskService.createRecurringTask(calendarId, request);
        return new ResponseEntity<>(createdTask, HttpStatus.CREATED);
    }

    @Operation(summary = "Lấy tất cả công việc lặp lại trong một lịch cụ thể")
    @GetMapping("/api/calendars/{calendarId}/recurring-tasks")
    public ResponseEntity<List<RecurringTaskDto.Response>> getAllRecurringTasksInCalendar(@PathVariable Long calendarId) {
        List<RecurringTaskDto.Response> tasks = recurringTaskService.getAllRecurringTasksInCalendar(calendarId);
        return ResponseEntity.ok(tasks);
    }

    @Operation(summary = "Lấy thông tin chi tiết một công việc lặp lại theo ID")
    @GetMapping("/api/recurring-tasks/{recurringTaskId}")
    public ResponseEntity<RecurringTaskDto.Response> getRecurringTaskById(@PathVariable Long recurringTaskId) {
        RecurringTaskDto.Response task = recurringTaskService.getRecurringTaskById(recurringTaskId);
        return ResponseEntity.ok(task);
    }

    @Operation(summary = "Cập nhật một công việc lặp lại theo ID")
    @PutMapping("/api/recurring-tasks/{recurringTaskId}")
    public ResponseEntity<RecurringTaskDto.Response> updateRecurringTask(
            @PathVariable Long recurringTaskId,
            @Valid @RequestBody RecurringTaskDto.Request request) {
        RecurringTaskDto.Response updatedTask = recurringTaskService.updateRecurringTask(recurringTaskId, request);
        return ResponseEntity.ok(updatedTask);
    }

    @Operation(summary = "Xóa một công việc lặp lại theo ID")
    @DeleteMapping("/api/recurring-tasks/{recurringTaskId}")
    public ResponseEntity<Void> deleteRecurringTask(@PathVariable Long recurringTaskId) {
        recurringTaskService.deleteRecurringTask(recurringTaskId);
        return ResponseEntity.noContent().build();
    }

}
