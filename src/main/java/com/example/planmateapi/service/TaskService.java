package com.example.planmateapi.service;

import com.example.planmateapi.dto.CreateTaskResponseDto;
import com.example.planmateapi.dto.TagDto;
import com.example.planmateapi.dto.TaskRequestDto;
import com.example.planmateapi.dto.TaskResponseDto;
import com.example.planmateapi.entity.*;
import com.example.planmateapi.entity.Calendar;
import com.example.planmateapi.exception.ResourceNotFoundException;
import com.example.planmateapi.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final RecurringTaskRepository recurringTaskRepository;
    private final CalendarRepository calendarRepository;
    private final TagRepository tagRepository;
    private final AuthenticationService authenticationService;
    private final CalendarShareRepository calendarShareRepository;

    @Transactional
    public void createOrUpdateTask(Long calendarId, Long taskId, TaskRequestDto request) {
        // SỬA LOGIC KIỂM TRA QUYỀN:
        if (!hasPermission(calendarId, PermissionLevel.EDIT)) {
            throw new AccessDeniedException("Bạn không có quyền thêm hoặc sửa công việc trong lịch này.");
        }

        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        Calendar calendar = calendarRepository.findById(calendarId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch: " + calendarId));

        // (Phần logic còn lại giữ nguyên)
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
            task.setCreatedBy(currentUser); // Vẫn lưu người tạo, nhưng không dùng để check quyền
            task.setTags(tags);
            task.setPreDayNotify(request.isPreDayNotify());
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
            recurringTask.setCreatedBy(currentUser); // Vẫn lưu người tạo, nhưng không dùng để check quyền
            recurringTask.setTags(tags);
            recurringTask.setPreDayNotify(request.isPreDayNotify());
            recurringTaskRepository.save(recurringTask);
        }
    }

    @Transactional
    public void deleteTask(Long taskId, String type) {
        // --- LOGIC SỬA LỖI CHÍNH Ở ĐÂY ---

        Long calendarId;

        // 1. Tìm task và lấy calendarId của nó
        if ("SINGLE".equalsIgnoreCase(type)) {
            Task task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công việc: " + taskId));
            calendarId = task.getCalendar().getId();

            // BỎ KIỂM TRA QUYỀN CŨ (task.getCreatedBy())

        } else if ("RECURRING".equalsIgnoreCase(type)) {
            RecurringTask recurringTask = recurringTaskRepository.findById(taskId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công việc lặp lại: " + taskId));
            calendarId = recurringTask.getCalendar().getId();

            // BỎ KIỂM TRA QUYỀN CŨ (recurringTask.getCreatedBy())

        } else {
            throw new IllegalArgumentException("Loại công việc không hợp lệ: " + type);
        }

        // 2. Kiểm tra quyền trên LỊCH (CALENDAR)
        if (!hasPermission(calendarId, PermissionLevel.EDIT)) {
            throw new AccessDeniedException("Bạn không có quyền xóa công việc này.");
        }

        // 3. Nếu có quyền, tiến hành xóa
        if ("SINGLE".equalsIgnoreCase(type)) {
            taskRepository.deleteById(taskId);
        } else {
            recurringTaskRepository.deleteById(taskId);
        }
    }

    public List<TaskResponseDto> getAllTasksInCalendar(Long calendarId) {
        // SỬA LOGIC KIỂM TRA QUYỀN:
        if (!hasPermission(calendarId, PermissionLevel.VIEW_ONLY)) {
            throw new AccessDeniedException("Bạn không có quyền xem lịch này.");
        }

        // (Bỏ các dòng kiểm tra owner cũ)

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
                .preDayNotify(task.isPreDayNotify())
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
                .preDayNotify(task.isPreDayNotify())
                .build();
    }

    private Set<TagDto.Response> mapTagsToDto(Set<Tag> tags) {
        if (tags == null) return new HashSet<>();
        return tags.stream()
                .map(tag -> new TagDto.Response(tag.getId(), tag.getName(), tag.getColor()))
                .collect(Collectors.toSet());
    }

    private boolean hasPermission(Long calendarId, PermissionLevel requiredPermission) {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();

        Optional<Calendar> calendarOpt = calendarRepository.findById(calendarId);
        if (calendarOpt.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy lịch: " + calendarId);
        }
        // 1. Kiểm tra nếu là chủ sở hữu
        if (calendarOpt.get().getOwner().getId().equals(currentUser.getId())) {
            return true;
        }

        // 2. Nếu không phải chủ, kiểm tra xem có được chia sẻ không
        CalendarShare share = calendarShareRepository
                .findByCalendarIdAndSharedWithUserId(calendarId, currentUser.getId())
                .orElse(null);

        if (share == null) {
            return false; // Không được chia sẻ
        }

        if (requiredPermission == PermissionLevel.VIEW_ONLY) {
            return true; // Chỉ cần xem (VIEW_ONLY hoặc EDIT đều được)
        }
        if (requiredPermission == PermissionLevel.EDIT) {
            return share.getPermissionLevel() == PermissionLevel.EDIT; // Phải là quyền EDIT
        }
        return false;
    }

    /**
     * Phương thức này được thiết kế riêng cho AI gọi.
     * Nó sẽ tự động tìm lịch mặc định của người dùng và tạo một công việc MỚI.
     */
    @Transactional
    public CreateTaskResponseDto createSingleTaskFromAi(String title, OffsetDateTime startTime, OffsetDateTime endTime, boolean preDayNotify) {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();

        // 1. Tìm lịch (calendar) mặc định của người dùng
        Calendar defaultCalendar = calendarRepository.findByOwnerId(currentUser.getId())
                .stream()
                .filter(Calendar::isDefault)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy lịch mặc định để thêm công việc."));

        // 2. Tạo TaskRequestDto để gọi lại hàm createOrUpdateTask
        TaskRequestDto request = new TaskRequestDto();
        request.setTitle(title);
        request.setStartTime(startTime);
        request.setEndTime(endTime);
        request.setRepeatType(RepeatType.NONE); // AI mặc định tạo task đơn
        request.setAllDay(false);
        request.setPreDayNotify(preDayNotify);

        // 3. Gọi hàm nghiệp vụ chính (đã có sẵn)
        // Chúng ta truyền null cho taskId để đảm bảo đây là lệnh TẠO MỚI
        createOrUpdateTask(defaultCalendar.getId(), null, request);

        // 4. Trả về DTO xác nhận
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM");
        String message = String.format("Đã đặt lịch '%s' trên lịch '%s' vào lúc %s.",
                title,
                defaultCalendar.getName(),
                startTime.format(DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM"))
        );

        return new CreateTaskResponseDto(true, message, title, startTime, defaultCalendar.getName());
    }

    @Transactional
    public CreateTaskResponseDto createRecurringTaskFromAi(
            String title, RepeatType repeatType, LocalTime startTime, LocalTime endTime,
            LocalDate startDate, String repeatDays, int repeatInterval, boolean preDayNotify
    ) {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();

        // 1. Tìm lịch default
        Calendar defaultCalendar = calendarRepository.findByOwnerId(currentUser.getId())
                .stream()
                .filter(Calendar::isDefault)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy lịch mặc định."));

        // 2. Xây dựng request cho hàm nghiệp vụ chính
        TaskRequestDto request = new TaskRequestDto();
        request.setTitle(title);
        request.setRepeatType(repeatType);
        request.setRepeatStartTime(startTime);
        request.setRepeatEndTime(endTime);
        request.setRepeatStart(startDate);
        request.setRepeatInterval(repeatInterval);
        request.setRepeatDays(repeatDays); // "MO,TU,WE"
        request.setTimezone("Asia/Ho_Chi_Minh"); // Hoặc lấy từ user profile nếu có
        request.setPreDayNotify(preDayNotify);

        // 3. Gọi hàm nghiệp vụ chính
        createOrUpdateTask(defaultCalendar.getId(), null, request);

        // 4. Trả về xác nhận
        String message = String.format("Đã tạo lịch lặp lại '%s' trên lịch '%s' (bắt đầu từ %s).",
                title,
                defaultCalendar.getName(),
                startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        );

        return new CreateTaskResponseDto(true, message, title, null, defaultCalendar.getName());
    }

    // 1. Hàm tìm kiếm để AI xác nhận với người dùng
    @Transactional(readOnly = true)
    public String findTasksForAi(String keyword) {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();

        // Lấy tất cả lịch của User
        List<Long> calendarIds = calendarRepository.findByOwnerId(currentUser.getId())
                .stream()
                .map(Calendar::getId)
                .toList();

        if (calendarIds.isEmpty()) return "Bạn chưa có lịch nào.";

        List<String> results = new ArrayList<>();
        ZoneOffset vietnamOffset = ZoneOffset.of("+07:00");

        boolean foundAny = false;

        // --- 1. TÌM TRONG BẢNG RECURRING TASK (Ưu tiên số 1) ---
        for (Long calId : calendarIds) {
            List<RecurringTask> rTasks = recurringTaskRepository.findByCalendarIdAndTitleContainingIgnoreCase(calId, keyword);
            for (RecurringTask rt : rTasks) {
                foundAny = true;
                // FIX: Thêm hiển thị rt.getRepeatDays() để biết lặp vào thứ mấy (VD: "SA,SU")
                String repeatInfo = String.format("%s (Lặp vào: %s)", rt.getRepeatType(), rt.getRepeatDays() != null ? rt.getRepeatDays() : "Hàng ngày");

                results.add(String.format("[ID: %d | LOẠI: RECURRING] - %s \n   -> Chi tiết: %s lúc %s",
                        rt.getId(), rt.getTitle(), repeatInfo, rt.getStartTime()));
            }
        }

        // --- 2. TÌM TRONG BẢNG TASK THƯỜNG ---
        for (Long calId : calendarIds) {
            List<Task> tasks = taskRepository.findByCalendarIdAndTitleContainingIgnoreCase(calId, keyword);
            for (Task t : tasks) {
                foundAny = true;
                String timeStr = t.getStartTime()
                        .withOffsetSameInstant(vietnamOffset)
                        .format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));

                results.add(String.format("[ID: %d | LOẠI: SINGLE] - %s (Diễn ra lúc: %s)",
                        t.getId(), t.getTitle(), timeStr));
            }
        }

        if (!foundAny) {
            return "Không tìm thấy công việc nào có tên chứa '" + keyword + "'.";
        }

        return String.join("\n", results);
    }

    // 2. Hàm xóa (AI gọi sau khi user confirm)
    @Transactional
    public String deleteTaskFromAi(Long id, String type) {
        try {
            deleteTask(id, type); // Gọi lại hàm deleteTask có sẵn của bạn (đã có check quyền)
            return "Đã xóa thành công công việc ID " + id;
        } catch (Exception e) {
            return "Lỗi khi xóa: " + e.getMessage();
        }
    }

    // 3. Hàm sửa (AI gọi sau khi user confirm) - Chỉ cho sửa Title và Thời gian đơn giản
    @Transactional
    public String editAnyTaskFromAi(Long id, String type, String newTitle, String newStartTimeISO, String newEndTimeISO) {
        try {
            User currentUser = authenticationService.getCurrentAuthenticatedUser();

            if ("SINGLE".equalsIgnoreCase(type)) {
                // --- XỬ LÝ TASK THƯỜNG (Logic cũ) ---
                Task task = taskRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Task thường ID " + id));

                if (!task.getCalendar().getOwner().getId().equals(currentUser.getId())) {
                    return "Bạn không có quyền sửa task này.";
                }

                if (newTitle != null && !newTitle.isEmpty()) task.setTitle(newTitle);
                // Parse ISO 8601 đầy đủ (VD: 2025-11-20T09:00:00+07:00)
                if (newStartTimeISO != null) task.setStartTime(OffsetDateTime.parse(newStartTimeISO));
                if (newEndTimeISO != null) task.setEndTime(OffsetDateTime.parse(newEndTimeISO));

                taskRepository.save(task);
                return "Đã cập nhật công việc thường thành công.";

            } else if ("RECURRING".equalsIgnoreCase(type)) {
                // --- XỬ LÝ TASK LẶP (Logic mới bổ sung) ---
                RecurringTask rTask = recurringTaskRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Task lặp ID " + id));

                if (!rTask.getCalendar().getOwner().getId().equals(currentUser.getId())) {
                    return "Bạn không có quyền sửa task này.";
                }

                if (newTitle != null && !newTitle.isEmpty()) rTask.setTitle(newTitle);

                // Với Task lặp, ta chỉ lấy phần GIỜ (LocalTime) từ chuỗi ISO
                if (newStartTimeISO != null) {
                    OffsetDateTime odt = OffsetDateTime.parse(newStartTimeISO);
                    rTask.setStartTime(odt.toLocalTime()); // Lấy 09:00:00
                }
                if (newEndTimeISO != null) {
                    OffsetDateTime odt = OffsetDateTime.parse(newEndTimeISO);
                    rTask.setEndTime(odt.toLocalTime()); // Lấy 10:00:00
                }

                recurringTaskRepository.save(rTask);
                return "Đã cập nhật lịch lặp lại thành công (đã đổi giờ/tên cho toàn bộ chuỗi lặp).";
            } else {
                return "Loại task không hợp lệ (phải là SINGLE hoặc RECURRING).";
            }
        } catch (Exception e) {
            return "Lỗi khi sửa: " + e.getMessage();
        }
    }
}