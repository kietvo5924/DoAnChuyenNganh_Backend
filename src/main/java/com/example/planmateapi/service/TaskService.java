package com.example.planmateapi.service;

import com.example.planmateapi.dto.TagDto;
import com.example.planmateapi.dto.TaskDto;
import com.example.planmateapi.entity.Calendar;
import com.example.planmateapi.entity.Tag;
import com.example.planmateapi.entity.Task;
import com.example.planmateapi.entity.User;
import com.example.planmateapi.exception.ResourceNotFoundException;
import com.example.planmateapi.repository.CalendarRepository;
import com.example.planmateapi.repository.TagRepository;
import com.example.planmateapi.repository.TaskRepository;
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
public class TaskService {

    private final TaskRepository taskRepository;
    private final CalendarRepository calendarRepository;
    private final TagRepository tagRepository;
    private final AuthenticationService authenticationService;

    @Transactional
    public TaskDto.Response createTask(Long calendarId, TaskDto.Request request) {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();

        // Tìm Calendar và xác thực quyền sở hữu
        Calendar calendar = calendarRepository.findById(calendarId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch với ID: " + calendarId));
        if (!calendar.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Bạn không có quyền thêm công việc vào lịch này.");
        }

        // Xử lý các Tag
        Set<Tag> tags = new HashSet<>();
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            tags = tagRepository.findByIdIn(request.getTagIds());
            for (Tag tag : tags) {
                if (!tag.getUser().getId().equals(currentUser.getId())) {
                    throw new AccessDeniedException("Bạn không có quyền sử dụng nhãn với ID: " + tag.getId());
                }
            }
        }

        Task newTask = new Task();
        newTask.setTitle(request.getTitle());
        newTask.setDescription(request.getDescription());
        newTask.setStartTime(request.getStartTime());
        newTask.setEndTime(request.getEndTime());
        newTask.setAllDay(request.isAllDay());
        newTask.setCalendar(calendar);
        newTask.setCreatedBy(currentUser);
        newTask.setTags(tags);

        Task savedTask = taskRepository.save(newTask);
        return toResponse(savedTask);
    }

    public List<TaskDto.Response> getAllTasksInCalendar(Long calendarId) {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        Calendar calendar = calendarRepository.findById(calendarId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch với ID: " + calendarId));
        if (!calendar.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Bạn không có quyền xem các công việc trong lịch này.");
        }

        return taskRepository.findByCalendarId(calendarId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public TaskDto.Response getTaskById(Long taskId) {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công việc với ID: " + taskId));

        if (!task.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Bạn không có quyền xem công việc này.");
        }

        return toResponse(task);
    }

    @Transactional
    public TaskDto.Response updateTask(Long taskId, TaskDto.Request request) {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công việc với ID: " + taskId));

        if (!task.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Bạn không có quyền sửa công việc này.");
        }

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStartTime(request.getStartTime());
        task.setEndTime(request.getEndTime());
        task.setAllDay(request.isAllDay());

        // Cập nhật danh sách tags
        if (request.getTagIds() != null) {
            Set<Tag> tags = tagRepository.findByIdIn(request.getTagIds());
            for (Tag tag : tags) {
                if (!tag.getUser().getId().equals(currentUser.getId())) {
                    throw new AccessDeniedException("Bạn không có quyền sử dụng nhãn với ID: " + tag.getId());
                }
            }
            task.setTags(tags);
        }

        Task updatedTask = taskRepository.save(task);
        return toResponse(updatedTask);
    }

    @Transactional
    public void deleteTask(Long taskId) {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công việc với ID: " + taskId));

        if (!task.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Bạn không có quyền xóa công việc này.");
        }

        taskRepository.delete(task);
    }

    private TaskDto.Response toResponse(Task task) {
        if (task == null) {
            return null;
        }

        TaskDto.Response response = new TaskDto.Response();
        response.setId(task.getId());
        response.setTitle(task.getTitle());
        response.setDescription(task.getDescription());
        response.setStartTime(task.getStartTime());
        response.setEndTime(task.getEndTime());
        response.setAllDay(task.isAllDay());

        if (task.getTags() != null) {
            response.setTags(task.getTags().stream()
                    .map(tag -> new TagDto.Response(tag.getId(), tag.getName(), tag.getColor()))
                    .collect(Collectors.toSet()));
        }

        return response;
    }
}
