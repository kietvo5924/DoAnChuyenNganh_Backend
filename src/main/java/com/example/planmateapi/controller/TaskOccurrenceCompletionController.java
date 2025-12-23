package com.example.planmateapi.controller;

import com.example.planmateapi.dto.TaskOccurrenceCompletionDto;
import com.example.planmateapi.entity.TaskType;
import com.example.planmateapi.service.TaskOccurrenceCompletionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Task Occurrence Completion")
@SecurityRequirement(name = "bearerAuth")
public class TaskOccurrenceCompletionController {

    private final TaskOccurrenceCompletionService service;

    @Operation(summary = "Đánh dấu hoàn thành một công việc theo ngày (áp dụng cho cả task thường và lặp)")
    @PutMapping("/api/tasks/{taskId}/occurrences/{date}/complete")
    public ResponseEntity<Void> setComplete(
            @PathVariable Long taskId,
            @PathVariable String date,
            @RequestParam TaskType type) {
        service.setCompleted(taskId, type, LocalDate.parse(date));
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Bỏ đánh dấu hoàn thành theo ngày")
    @DeleteMapping("/api/tasks/{taskId}/occurrences/{date}/complete")
    public ResponseEntity<Void> clearComplete(
            @PathVariable Long taskId,
            @PathVariable String date,
            @RequestParam TaskType type) {
        service.clearCompleted(taskId, type, LocalDate.parse(date));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Lấy danh sách hoàn thành trong một lịch theo khoảng ngày")
    @GetMapping("/api/calendars/{calendarId}/occurrences/completions")
    public ResponseEntity<List<TaskOccurrenceCompletionDto>> getCompletions(
            @PathVariable Long calendarId,
            @RequestParam String from,
            @RequestParam(required = false) String to) {
        final LocalDate f = LocalDate.parse(from);
        final LocalDate t = (to == null || to.isBlank()) ? f : LocalDate.parse(to);
        return ResponseEntity.ok(service.getCompletions(calendarId, f, t));
    }
}
