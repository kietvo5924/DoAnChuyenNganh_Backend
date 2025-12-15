package com.example.planmateapi.service;

import com.example.planmateapi.dto.TaskOccurrenceCompletionDto;
import com.example.planmateapi.entity.*;
import com.example.planmateapi.entity.Calendar;
import com.example.planmateapi.exception.ResourceNotFoundException;
import com.example.planmateapi.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskOccurrenceCompletionService {

    private final TaskOccurrenceCompletionRepository completionRepository;
    private final TaskRepository taskRepository;
    private final RecurringTaskRepository recurringTaskRepository;
    private final CalendarRepository calendarRepository;
    private final CalendarShareRepository calendarShareRepository;
    private final AuthenticationService authenticationService;

    @Transactional
    public void setCompleted(Long taskId, TaskType taskType, LocalDate date) {
        final Long calendarId = resolveCalendarId(taskId, taskType);

        if (!hasPermission(calendarId, PermissionLevel.EDIT)) {
            throw new AccessDeniedException("Bạn không có quyền cập nhật trạng thái hoàn thành trong lịch này.");
        }

        final TaskOccurrenceCompletion completion = TaskOccurrenceCompletion.of(
                calendarId,
                taskId,
                taskType,
                date);
        completionRepository.save(completion);
    }

    @Transactional
    public void clearCompleted(Long taskId, TaskType taskType, LocalDate date) {
        final Long calendarId = resolveCalendarId(taskId, taskType);

        if (!hasPermission(calendarId, PermissionLevel.EDIT)) {
            throw new AccessDeniedException("Bạn không có quyền cập nhật trạng thái hoàn thành trong lịch này.");
        }

        completionRepository.deleteById(new TaskOccurrenceCompletionId(taskId, taskType, date));
    }

    @Transactional(readOnly = true)
    public List<TaskOccurrenceCompletionDto> getCompletions(Long calendarId, LocalDate from, LocalDate to) {
        if (!hasPermission(calendarId, PermissionLevel.VIEW_ONLY)) {
            throw new AccessDeniedException("Bạn không có quyền xem lịch này.");
        }

        return completionRepository
                .findByCalendarIdAndIdOccurrenceDateBetween(calendarId, from, to)
                .stream()
                .map(c -> TaskOccurrenceCompletionDto.builder()
                        .calendarId(c.getCalendarId())
                        .taskId(c.getId().getTaskId())
                        .taskType(c.getId().getTaskType())
                        .occurrenceDate(c.getId().getOccurrenceDate())
                        .completed(c.isCompleted())
                        .build())
                .collect(Collectors.toList());
    }

    private Long resolveCalendarId(Long taskId, TaskType taskType) {
        if (taskType == TaskType.SINGLE) {
            final Task t = taskRepository.findById(taskId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công việc: " + taskId));
            return t.getCalendar().getId();
        }
        final RecurringTask rt = recurringTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công việc lặp lại: " + taskId));
        return rt.getCalendar().getId();
    }

    private boolean hasPermission(Long calendarId, PermissionLevel requiredPermission) {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();

        Optional<Calendar> calendarOpt = calendarRepository.findById(calendarId);
        if (calendarOpt.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy lịch: " + calendarId);
        }
        if (calendarOpt.get().getOwner().getId().equals(currentUser.getId())) {
            return true;
        }

        CalendarShare share = calendarShareRepository
                .findByCalendarIdAndSharedWithUserId(calendarId, currentUser.getId())
                .orElse(null);

        if (share == null) {
            return false;
        }

        if (requiredPermission == PermissionLevel.VIEW_ONLY) {
            return true;
        }
        if (requiredPermission == PermissionLevel.EDIT) {
            return share.getPermissionLevel() == PermissionLevel.EDIT;
        }
        return false;
    }
}
