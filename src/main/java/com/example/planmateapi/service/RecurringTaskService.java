package com.example.planmateapi.service;

import com.example.planmateapi.dto.RecurringTaskDto;
import com.example.planmateapi.dto.TagDto;
import com.example.planmateapi.entity.Calendar;
import com.example.planmateapi.entity.RecurringTask;
import com.example.planmateapi.entity.Tag;
import com.example.planmateapi.entity.User;
import com.example.planmateapi.exception.ResourceNotFoundException;
import com.example.planmateapi.repository.CalendarRepository;
import com.example.planmateapi.repository.RecurringTaskRepository;
import com.example.planmateapi.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecurringTaskService {

    private final RecurringTaskRepository recurringTaskRepository;
    private final TagRepository tagRepository;
    private final CalendarRepository calendarRepository;
    private final AuthenticationService authenticationService;

    @Transactional
    public RecurringTaskDto.Response createRecurringTask(Long calendarId, RecurringTaskDto.Request request) {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();

        Calendar calendar = calendarRepository.findById(calendarId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch với ID: " + calendarId));
        if (!calendar.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Bạn không có quyền thêm công việc vào lịch này.");
        }

        Set<Tag> tags = new HashSet<>();
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            tags = tagRepository.findByIdIn(request.getTagIds());
            for (Tag tag : tags) {
                if (!tag.getUser().getId().equals(currentUser.getId())) {
                    throw new AccessDeniedException("Bạn không có quyền sử dụng nhãn với ID: " + tag.getId());
                }
            }
        }

        RecurringTask newTask = new RecurringTask();
        newTask.setTitle(request.getTitle());
        newTask.setDescription(request.getDescription());
        newTask.setStartTime(request.getStartTime());
        newTask.setEndTime(request.getEndTime());
        newTask.setTimezone(request.getTimezone());
        newTask.setRepeatType(request.getRepeatType());
        newTask.setRepeatInterval(request.getRepeatInterval());
        newTask.setRepeatDays(request.getRepeatDays());
        newTask.setRepeatDayOfMonth(request.getRepeatDayOfMonth());
        newTask.setRepeatWeekOfMonth(request.getRepeatWeekOfMonth());
        newTask.setRepeatDayOfWeek(request.getRepeatDayOfWeek());
        newTask.setRepeatStart(request.getRepeatStart());
        newTask.setRepeatEnd(request.getRepeatEnd());
        newTask.setExceptions(request.getExceptions());
        newTask.setTags(tags);
        newTask.setCalendar(calendar);
        newTask.setCreatedBy(currentUser);

        RecurringTask savedTask = recurringTaskRepository.save(newTask);
        return toResponse(savedTask);
    }

    public List<RecurringTaskDto.Response> getAllRecurringTasksInCalendar(Long calendarId) {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        Calendar calendar = calendarRepository.findById(calendarId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch với ID: " + calendarId));

        if (!calendar.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Bạn không có quyền xem các công việc trong lịch này.");
        }

        return recurringTaskRepository.findByCalendarId(calendarId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public RecurringTaskDto.Response getRecurringTaskById(Long recurringTaskId) {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        RecurringTask task = recurringTaskRepository.findById(recurringTaskId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công việc với ID: " + recurringTaskId));

        if (!task.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Bạn không có quyền xem công việc này.");
        }

        return toResponse(task);
    }

    @Transactional
    public RecurringTaskDto.Response updateRecurringTask(Long recurringTaskId, RecurringTaskDto.Request request) {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        RecurringTask task = recurringTaskRepository.findById(recurringTaskId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công việc với ID: " + recurringTaskId));

        if (!task.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Bạn không có quyền sửa công việc này.");
        }

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStartTime(request.getStartTime());
        task.setEndTime(request.getEndTime());
        task.setTimezone(request.getTimezone());
        task.setRepeatType(request.getRepeatType());
        task.setRepeatInterval(request.getRepeatInterval());
        task.setRepeatDays(request.getRepeatDays());
        task.setRepeatDayOfMonth(request.getRepeatDayOfMonth());
        task.setRepeatWeekOfMonth(request.getRepeatWeekOfMonth());
        task.setRepeatDayOfWeek(request.getRepeatDayOfWeek());
        task.setRepeatStart(request.getRepeatStart());
        task.setRepeatEnd(request.getRepeatEnd());
        task.setExceptions(request.getExceptions());

        if (request.getTagIds() != null) {
            Set<Tag> tags = tagRepository.findByIdIn(request.getTagIds());
            for (Tag tag : tags) {
                if (!tag.getUser().getId().equals(currentUser.getId())) {
                    throw new AccessDeniedException("Bạn không có quyền sử dụng nhãn với ID: " + tag.getId());
                }
            }
            task.setTags(tags);
        }

        RecurringTask updatedRecurringTask = recurringTaskRepository.save(task);
        return toResponse(updatedRecurringTask);
    }

    @Transactional
    public void deleteRecurringTask(Long recurringTaskId) {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        RecurringTask task = recurringTaskRepository.findById(recurringTaskId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công việc lặp lại với ID: " + recurringTaskId));
        if (!task.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Bạn không có quyền xóa công việc này.");
        }

        recurringTaskRepository.delete(task);
    }

    private RecurringTaskDto.Response toResponse(RecurringTask task) {
        RecurringTaskDto.Response response = new RecurringTaskDto.Response();
        response.setId(task.getId());
        response.setTitle(task.getTitle());
        response.setDescription(task.getDescription());
        response.setStartTime(task.getStartTime());
        response.setEndTime(task.getEndTime());
        response.setTimezone(task.getTimezone());
        response.setRepeatType(task.getRepeatType());
        response.setRepeatInterval(task.getRepeatInterval());
        response.setRepeatDays(task.getRepeatDays());
        response.setRepeatDayOfMonth(task.getRepeatDayOfMonth());
        response.setRepeatWeekOfMonth(task.getRepeatWeekOfMonth());
        response.setRepeatDayOfWeek(task.getRepeatDayOfWeek());
        response.setRepeatStart(task.getRepeatStart());
        response.setRepeatEnd(task.getRepeatEnd());
        response.setExceptions(task.getExceptions());

        if (task.getTags() != null) {
            response.setTags(task.getTags().stream()
                    .map(tag -> new TagDto.Response(tag.getId(), tag.getName(), tag.getColor()))
                    .collect(Collectors.toSet()));
        }

        return response;
    }
}
