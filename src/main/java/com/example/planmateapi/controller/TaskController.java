package com.example.planmateapi.controller;

import com.example.planmateapi.dto.TaskDto;
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
@Tag(name = "Task API")
@SecurityRequirement(name = "bearerAuth")
public class TaskController {

    private final TaskService taskService;

    @Operation(summary = "Tạo một công việc mới trong một lịch cụ thể")
    @PostMapping("/api/calendars/{calendarId}/tasks")
    public ResponseEntity<TaskDto.Response> createTask(
            @PathVariable Long calendarId,
            @Valid @RequestBody TaskDto.Request request) {
        TaskDto.Response createdTask = taskService.createTask(calendarId, request);
        return new ResponseEntity<>(createdTask, HttpStatus.CREATED);
    }

    @Operation(summary = "Lấy tất cả công việc trong một lịch cụ thể")
    @GetMapping("/api/calendars/{calendarId}/tasks")
    public ResponseEntity<List<TaskDto.Response>> getAllTasksInCalendar(@PathVariable Long calendarId) {
        List<TaskDto.Response> tasks = taskService.getAllTasksInCalendar(calendarId);
        return ResponseEntity.ok(tasks);
    }

    @Operation(summary = "Lấy thông tin chi tiết một công việc theo ID")
    @GetMapping("/api/tasks/{taskId}")
    public ResponseEntity<TaskDto.Response> getTaskById(@PathVariable Long taskId) {
        TaskDto.Response task = taskService.getTaskById(taskId);
        return ResponseEntity.ok(task);
    }

    @Operation(summary = "Cập nhật một công việc theo ID")
    @PutMapping("/api/tasks/{taskId}")
    public ResponseEntity<TaskDto.Response> updateTask(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskDto.Request request) {
        TaskDto.Response updatedTask = taskService.updateTask(taskId, request);
        return ResponseEntity.ok(updatedTask);
    }

    @Operation(summary = "Xóa một công việc theo ID")
    @DeleteMapping("/api/tasks/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long taskId) {
        taskService.deleteTask(taskId);
        return ResponseEntity.noContent().build();
    }
}
