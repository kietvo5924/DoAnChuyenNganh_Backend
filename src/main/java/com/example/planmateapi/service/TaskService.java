package com.example.planmateapi.service;

import com.example.planmateapi.dto.TagDto;
import com.example.planmateapi.dto.TaskRequestDto;
import com.example.planmateapi.dto.TaskResponseDto;
import com.example.planmateapi.entity.*;
import com.example.planmateapi.exception.ResourceNotFoundException;
import com.example.planmateapi.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final RecurringTaskRepository recurringTaskRepository;
    private final CalendarRepository calendarRepository;
    private final TagRepository tagRepository;
    private final AuthenticationService authenticationService;

    @Transactional
    public void createOrUpdateTask(Long calendarId, Long taskId, TaskRequestDto request) {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        Calendar calendar = calendarRepository.findById(calendarId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch: " + calendarId));

        if (!calendar.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Bạn không có quyền truy cập vào lịch này.");
        }

        Set<Tag> tags = new HashSet<>();
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            tags = tagRepository.findByIdIn(request.getTagIds());
        }

        boolean isCreatingNew = (taskId == null);

        // --- Logic phân biệt chính ---
        if (request.getRepeatType() == RepeatType.NONE) {
            // Xử lý cho Task thường
            Task task;
            if (isCreatingNew) {
                task = new Task();
            } else {
                task = taskRepository.findById(taskId)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công việc: " + taskId));
                // Logic chuyển đổi: Nếu task cũ là recurring, xóa nó đi
                recurringTaskRepository.deleteById(taskId);
            }

            task.setTitle(request.getTitle());
            task.setDescription(request.getDescription());
            task.setStartTime(request.getStartTime());
            task.setEndTime(request.getEndTime());
            task.setAllDay(request.isAllDay());
            task.setCalendar(calendar);
            task.setCreatedBy(currentUser);
            task.setTags(tags);
            taskRepository.save(task);

        } else {
            // Xử lý cho Recurring Task
            RecurringTask recurringTask;
            if (isCreatingNew) {
                recurringTask = new RecurringTask();
            } else {
                recurringTask = recurringTaskRepository.findById(taskId)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công việc lặp lại: " + taskId));
                // Logic chuyển đổi: Nếu task cũ là task thường, xóa nó đi
                taskRepository.deleteById(taskId);
            }

            recurringTask.setTitle(request.getTitle());
            recurringTask.setDescription(request.getDescription());
            recurringTask.setRepeatType(request.getRepeatType());
            recurringTask.setRepeatStart(request.getRepeatStart());
            recurringTask.setRepeatEnd(request.getRepeatEnd());
            recurringTask.setStartTime(request.getRepeatStartTime());
            recurringTask.setEndTime(request.getRepeatEndTime());
            recurringTask.setTimezone(request.getTimezone());
            recurringTask.setRepeatInterval(request.getRepeatInterval());
            recurringTask.setRepeatDays(request.getRepeatDays());
            recurringTask.setRepeatDayOfMonth(request.getRepeatDayOfMonth());
            recurringTask.setRepeatWeekOfMonth(request.getRepeatWeekOfMonth());
            recurringTask.setRepeatDayOfWeek(recurringTask.getRepeatWeekOfMonth());
            recurringTask.setExceptions(request.getExceptions());
            recurringTask.setCalendar(calendar);
            recurringTask.setCreatedBy(currentUser);
            recurringTask.setTags(tags);
            recurringTaskRepository.save(recurringTask);
        }
    }

    @Transactional
    public void deleteTask(Long taskId, String type) {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        if ("SINGLE".equalsIgnoreCase(type)) {
            Task task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công việc: " + taskId));
            if (!task.getCreatedBy().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("Bạn không có quyền xóa công việc này.");
            }
            taskRepository.deleteById(taskId);
        } else if ("RECURRING".equalsIgnoreCase(type)) {
            RecurringTask recurringTask = recurringTaskRepository.findById(taskId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công việc lặp lại: " + taskId));
            if (!recurringTask.getCreatedBy().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("Bạn không có quyền xóa công việc này.");
            }
            recurringTaskRepository.deleteById(taskId);
        } else {
            throw new IllegalArgumentException("Loại công việc không hợp lệ: " + type);
        }
    }

    public List<TaskResponseDto> getAllTasksInCalendar(Long calendarId) {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        Calendar calendar = calendarRepository.findById(calendarId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch: " + calendarId));
        if (!calendar.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Bạn không có quyền xem lịch này.");
        }

        List<TaskResponseDto> responseList = new ArrayList<>();

        // Lấy và chuyển đổi Task thường
        List<Task> tasks = taskRepository.findByCalendarId(calendarId);
        tasks.forEach(task -> responseList.add(mapTaskToResponse(task)));

        // Lấy và chuyển đổi Recurring Task
        List<RecurringTask> recurringTasks = recurringTaskRepository.findByCalendarId(calendarId);
        recurringTasks.forEach(task -> responseList.add(mapRecurringTaskToResponse(task)));

        return responseList;
    }

    // --- Các hàm Mapper nội bộ ---
    private TaskResponseDto mapTaskToResponse(Task task) {
        return TaskResponseDto.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .repeatType(RepeatType.NONE) // Loại là NONE
                .startTime(task.getStartTime())
                .endTime(task.getEndTime())
                .isAllDay(task.isAllDay())
                .tags(mapTagsToDto(task.getTags()))
                .build();
    }

    private TaskResponseDto mapRecurringTaskToResponse(RecurringTask task) {
        return TaskResponseDto.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .repeatType(task.getRepeatType())
                .repeatStartTime(task.getStartTime())
                .repeatEndTime(task.getEndTime())
                .repeatStart(task.getRepeatStart())
                .repeatEnd(task.getRepeatEnd())
                .timezone(task.getTimezone())
                .repeatInterval(task.getRepeatInterval())
                .repeatDays(task.getRepeatDays())
                .repeatDayOfMonth(task.getRepeatDayOfMonth())
                .repeatWeekOfMonth(task.getRepeatWeekOfMonth())
                .repeatDayOfWeek(task.getRepeatDayOfWeek())
                .exceptions(task.getExceptions())
                .tags(mapTagsToDto(task.getTags()))
                .build();
    }

    private Set<TagDto.Response> mapTagsToDto(Set<Tag> tags) {
        if (tags == null) return new HashSet<>();
        return tags.stream()
                .map(tag -> new TagDto.Response(tag.getId(), tag.getName(), tag.getColor()))
                .collect(Collectors.toSet());
    }
}