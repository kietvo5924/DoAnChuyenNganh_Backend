package com.example.planmateapi.service;

import com.example.planmateapi.dto.*;
import com.example.planmateapi.entity.*;
import com.example.planmateapi.entity.Calendar;
import com.example.planmateapi.exception.ResourceNotFoundException;
import com.example.planmateapi.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalendarShareService {

    private final CalendarRepository calendarRepository;
    private final CalendarShareRepository calendarShareRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final RecurringTaskRepository recurringTaskRepository;
    private final TagRepository tagRepository;
    private final AuthenticationService authenticationService;

    @Transactional
    public CalendarShare shareCalendar(Long calendarId, ShareRequestDto request) {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        Calendar calendar = findAndVerifyOwnership(calendarId, currentUser);

        User targetUser = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với email: " + request.getEmail()));

        if (currentUser.getId().equals(targetUser.getId())) {
            throw new IllegalArgumentException("Bạn không thể chia sẻ lịch cho chính mình.");
        }

        CalendarShare share = calendarShareRepository
                .findByCalendarIdAndSharedWithUserId(calendarId, targetUser.getId())
                .orElse(CalendarShare.builder()
                        .calendar(calendar)
                        .sharedWithUser(targetUser)
                        .build());

        share.setPermissionLevel(request.getPermissionLevel());
        return calendarShareRepository.save(share);
    }

    @Transactional
    public void unshareCalendar(Long calendarId, Long targetUserId) {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();

        findAndVerifyOwnership(calendarId, currentUser);

        if (!userRepository.existsById(targetUserId)){
            throw new ResourceNotFoundException("Không tìm thấy người dùng ID: " + targetUserId);
        }

        calendarShareRepository.deleteByCalendarIdAndSharedWithUserId(calendarId, targetUserId);
    }

    // Chủ sở hữu xem ai đang được chia sẻ
    public List<UserResponseDTO> getUsersSharingCalendar(Long calendarId) {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        findAndVerifyOwnership(calendarId, currentUser);

        List<CalendarShare> shares = calendarShareRepository.findByCalendarId(calendarId);
        return shares.stream()
                .map(share -> UserResponseDTO.fromUser(share.getSharedWithUser()))
                .collect(Collectors.toList());
    }

    // Lấy danh sách lịch ĐƯỢC CHIA SẺ VỚI TÔI
    public List<CalendarDto.Response> getCalendarsSharedWithMe() {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        List<CalendarShare> shares = calendarShareRepository.findBySharedWithUserId(currentUser.getId());
        return shares.stream()
                .map(share -> mapToCalendarResponse(share.getCalendar()))
                .collect(Collectors.toList());
    }

    // Lấy chi tiết 1 lịch
    public CalendarDto.Response getSharedCalendarById(Long calendarId) {
        if (!hasPermission(calendarId, PermissionLevel.VIEW_ONLY)) {
            throw new AccessDeniedException("Bạn không có quyền xem lịch này.");
        }
        Calendar calendar = calendarRepository.findById(calendarId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch: " + calendarId));
        return mapToCalendarResponse(calendar);
    }

    // Cập nhật 1 lịch
    public CalendarDto.Response updateSharedCalendar(Long calendarId, CalendarDto.Request request) {
        if (!hasPermission(calendarId, PermissionLevel.EDIT)) {
            throw new AccessDeniedException("Bạn không có quyền cập nhật lịch này.");
        }
        Calendar calendar = calendarRepository.findById(calendarId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch: " + calendarId));

        calendar.setName(request.getName());
        calendar.setDescription(request.getDescription());
        Calendar updatedCalendar = calendarRepository.save(calendar);
        return mapToCalendarResponse(updatedCalendar);
    }

    // Lấy tất cả nhiệm vụ trong 1 lịch
    public List<TaskResponseDto> getAllTasksInSharedCalendar(Long calendarId) {
        if (!hasPermission(calendarId, PermissionLevel.VIEW_ONLY)) {
            throw new AccessDeniedException("Bạn không có quyền xem các công việc trong lịch này.");
        }

        List<TaskResponseDto> responseList = new ArrayList<>();
        List<Task> tasks = taskRepository.findByCalendarId(calendarId);
        tasks.forEach(task -> responseList.add(mapTaskToResponse(task)));
        List<RecurringTask> recurringTasks = recurringTaskRepository.findByCalendarId(calendarId);
        recurringTasks.forEach(task -> responseList.add(mapRecurringTaskToResponse(task)));
        return responseList;
    }

    // Tạo hoặc sửa task trong lịch
    @Transactional
    public void createOrUpdateTaskInSharedCalendar(Long calendarId, Long taskId, TaskRequestDto request) {
        if (!hasPermission(calendarId, PermissionLevel.EDIT)) {
            throw new AccessDeniedException("Bạn không có quyền thêm hoặc sửa công việc trong lịch này.");
        }

        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        Calendar calendar = calendarRepository.findById(calendarId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch: " + calendarId));

        Set<Tag> tags = new HashSet<>();
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            tags = tagRepository.findByIdIn(request.getTagIds());
        }

        boolean isCreatingNew = (taskId == null);

        if (request.getRepeatType() == RepeatType.NONE) {
            // Xử lý cho Task thường
            Task task;
            if (isCreatingNew) {
                task = new Task();
            } else {
                task = taskRepository.findById(taskId)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công việc: " + taskId));
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
    public void deleteTaskInSharedCalendar(Long taskId, String type) {
        Long calendarId;

        if ("SINGLE".equalsIgnoreCase(type)) {
            Task task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công việc: " + taskId));
            calendarId = task.getCalendar().getId();
        } else if ("RECURRING".equalsIgnoreCase(type)) {
            RecurringTask recurringTask = recurringTaskRepository.findById(taskId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công việc lặp lại: " + taskId));
            calendarId = recurringTask.getCalendar().getId();
        } else {
            throw new IllegalArgumentException("Loại công việc không hợp lệ: " + type);
        }

        if (!hasPermission(calendarId, PermissionLevel.EDIT)) {
            throw new AccessDeniedException("Bạn không có quyền xóa công việc này.");
        }

        if ("SINGLE".equalsIgnoreCase(type)) {
            taskRepository.deleteById(taskId);
        } else {
            recurringTaskRepository.deleteById(taskId);
        }
    }

    // --- HÀM HELPER NỘI BỘ ---

    // Hàm kiểm tra quyền
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
            return share.getPermissionLevel() == PermissionLevel.EDIT; // Phải là quyền EDIT
        }
        return false;
    }

    private Calendar findAndVerifyOwnership(Long calendarId, User user) {
        Calendar calendar = calendarRepository.findById(calendarId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch với ID: " + calendarId));
        if (!calendar.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("Bạn không phải chủ sở hữu của lịch này.");
        }
        return calendar;
    }

    private CalendarDto.Response mapToCalendarResponse(Calendar calendar) {
        return new CalendarDto.Response(calendar.getId(), calendar.getName(), calendar.getDescription(), calendar.isDefault());
    }

    private TaskResponseDto mapTaskToResponse(Task task) {
        return TaskResponseDto.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .repeatType(RepeatType.NONE)
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
