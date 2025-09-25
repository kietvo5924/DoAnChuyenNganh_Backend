package com.example.planmateapi.controller;

import com.example.planmateapi.dto.TaskRequestDto;
import com.example.planmateapi.dto.TaskResponseDto;
import com.example.planmateapi.service.TaskService;
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
@Tag(name = "Unified Task API")
@SecurityRequirement(name = "bearerAuth")
public class TaskController {

    private final TaskService taskService;

    @Operation(summary = "Lấy tất cả công việc (thường và lặp lại) trong một lịch")
    @GetMapping("/api/calendars/{calendarId}/tasks")
    public ResponseEntity<List<TaskResponseDto>> getAllTasksInCalendar(@PathVariable Long calendarId) {
        List<TaskResponseDto> tasks = taskService.getAllTasksInCalendar(calendarId);
        return ResponseEntity.ok(tasks);
    }

    @Operation(summary = "Tạo một công việc mới (thường hoặc lặp lại)")
    @PostMapping("/api/calendars/{calendarId}/tasks")
    public ResponseEntity<Void> createTask(
            @PathVariable Long calendarId,
            @Valid @RequestBody TaskRequestDto request) {
        taskService.createOrUpdateTask(calendarId, null, request);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @Operation(summary = "Cập nhật một công việc (có thể chuyển đổi giữa thường và lặp lại)")
    @PutMapping("/api/tasks/{taskId}")
    public ResponseEntity<Void> updateTask(
            @PathVariable Long taskId,
            @RequestParam Long calendarId, // Cần calendarId để xác thực quyền
            @Valid @RequestBody TaskRequestDto request) {
        taskService.createOrUpdateTask(calendarId, taskId, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Xóa một công việc (thường hoặc lặp lại)")
    @DeleteMapping("/api/tasks/{taskId}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long taskId,
            @RequestParam String type // Gửi "SINGLE" hoặc "RECURRING"
    ) {
        taskService.deleteTask(taskId, type);
        return ResponseEntity.noContent().build();
    }
}