package com.example.planmateapi.service;

import com.example.planmateapi.dto.AvailabilityRequestDto;
import com.example.planmateapi.dto.AvailabilityResponseDto;
import com.example.planmateapi.entity.*;
import com.example.planmateapi.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AvailabilityService {

    private final AuthenticationService authenticationService;
    private final CalendarRepository calendarRepository;
    private final CalendarShareRepository calendarShareRepository;
    private final TaskRepository taskRepository;
    private final RecurringTaskRepository recurringTaskRepository;

    public AvailabilityResponseDto checkAvailability(AvailabilityRequestDto request) {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        Long currentUserId = currentUser.getId();

        // 1. Lấy tất cả ID lịch mà người dùng có quyền xem
        Set<Long> calendarIds = new HashSet<>();
        calendarRepository.findByOwnerId(currentUserId).forEach(cal -> calendarIds.add(cal.getId()));
        calendarShareRepository.findBySharedWithUserId(currentUserId).forEach(share -> calendarIds.add(share.getCalendar().getId()));

        if (calendarIds.isEmpty()) {
            return AvailabilityResponseDto.builder().isFree(true).message("Rảnh (không có lịch nào).").build();
        }

        // 2. Tìm tất cả các xung đột
        List<AvailabilityResponseDto.ConflictInfo> conflicts = new ArrayList<>();
        LocalDate queryDate = request.getCheckStartTime().toLocalDate();

        for (Long calendarId : calendarIds) {
            Calendar calendar = calendarRepository.findById(calendarId).orElse(null);
            if (calendar == null) continue;

            // 2a. Kiểm tra Task (công việc đơn lẻ)
            List<Task> tasks = taskRepository.findByCalendarId(calendarId);
            for (Task task : tasks) {
                if (isOverlapping(task.getStartTime(), task.getEndTime(), request.getCheckStartTime(), request.getCheckEndTime())) {
                    conflicts.add(AvailabilityResponseDto.ConflictInfo.builder()
                            .taskTitle(task.getTitle())
                            .calendarName(calendar.getName())
                            .conflictStartTime(task.getStartTime())
                            .conflictEndTime(task.getEndTime())
                            .build());
                }
            }

            // 2b. Kiểm tra RecurringTask (công việc lặp lại)
            List<RecurringTask> recurringTasks = recurringTaskRepository.findByCalendarId(calendarId);
            for (RecurringTask rTask : recurringTasks) {
                if (doesRecurringTaskOccurOnDate(rTask, queryDate)) {
                    try {
                        ZoneOffset offset = request.getCheckStartTime().getOffset();
                        OffsetDateTime busyStart = OffsetDateTime.of(queryDate, rTask.getStartTime(), offset);
                        OffsetDateTime busyEnd = OffsetDateTime.of(queryDate, rTask.getEndTime(), offset);

                        if (isOverlapping(busyStart, busyEnd, request.getCheckStartTime(), request.getCheckEndTime())) {
                            conflicts.add(AvailabilityResponseDto.ConflictInfo.builder()
                                    .taskTitle(rTask.getTitle())
                                    .calendarName(calendar.getName())
                                    .conflictStartTime(busyStart)
                                    .conflictEndTime(busyEnd)
                                    .build());
                        }
                    } catch (Exception e) {
                        log.error("Lỗi khi tính toán thời gian lặp lại cho task ID {}: {}", rTask.getId(), e.getMessage());
                    }
                }
            }
        }

        // 3. Trả về kết quả
        if (conflicts.isEmpty()) {
            return AvailabilityResponseDto.builder().isFree(true).message("Bạn rảnh trong khoảng thời gian này.").conflicts(conflicts).build();
        } else {
            return AvailabilityResponseDto.builder()
                    .isFree(false)
                    .message("Bạn bận. Có " + conflicts.size() + " công việc bị trùng.")
                    .conflicts(conflicts)
                    .build();
        }
    }

    private boolean isOverlapping(OffsetDateTime startA, OffsetDateTime endA, OffsetDateTime startB, OffsetDateTime endB) {
        return startA.isBefore(endB) && startB.isBefore(endA);
    }

    private boolean doesRecurringTaskOccurOnDate(RecurringTask task, LocalDate date) {

        if (date.isBefore(task.getRepeatStart()) || (task.getRepeatEnd() != null && date.isAfter(task.getRepeatEnd()))) {
            return false;
        }

        int interval = task.getRepeatInterval() != null ? task.getRepeatInterval() : 1;
        LocalDate repeatStartDate = task.getRepeatStart();

        switch (task.getRepeatType()) {
            case DAILY:
                long daysBetween = ChronoUnit.DAYS.between(repeatStartDate, date);
                return daysBetween % interval == 0;

            case WEEKLY:
                long weeksBetween = ChronoUnit.WEEKS.between(repeatStartDate, date);
                if (weeksBetween % interval != 0) {
                    return false;
                }
                String dayAbbreviation = date.getDayOfWeek().name().substring(0, 2); // "MO", "TU", ...
                return task.getRepeatDays() != null && task.getRepeatDays().toUpperCase().contains(dayAbbreviation);

            case MONTHLY:
                // 1. Kiểm tra xem có đúng tháng theo "interval" không
                // Chuẩn hóa về ngày 1 để đếm số tháng một cách an toàn
                LocalDate startDateNormalized = repeatStartDate.withDayOfMonth(1);
                LocalDate queryDateNormalized = date.withDayOfMonth(1);
                long monthsBetween = ChronoUnit.MONTHS.between(startDateNormalized, queryDateNormalized);

                if (monthsBetween % interval != 0) {
                    return false;
                }

                // 2. Kiểm tra xem có đúng ngày trong tháng không
                int startDay = repeatStartDate.getDayOfMonth();
                int queryDay = date.getDayOfMonth();

                // 2a. Trường hợp ngày khớp trực tiếp (ví dụ: cùng là ngày 15)
                if (startDay == queryDay) {
                    return true;
                }

                // 2b. Xử lý trường hợp "ngày cuối cùng của tháng"
                // (ví dụ: bắt đầu vào ngày 31/01, sẽ lặp vào 28/02 hoặc 29/02)
                if (startDay >= 28) {
                    boolean isStartDayLastDay = (startDay == repeatStartDate.lengthOfMonth());
                    boolean isQueryDayLastDay = (queryDay == date.lengthOfMonth());

                    if (isStartDayLastDay && isQueryDayLastDay) {
                        return true;
                    }
                }
                return false;

            case YEARLY:
                // 1. Kiểm tra xem có đúng năm theo "interval" không
                long yearsBetween = ChronoUnit.YEARS.between(repeatStartDate, date);
                if (yearsBetween % interval != 0) {
                    return false;
                }

                // 2. Kiểm tra xem có đúng ngày và tháng không
                // 2a. Trường hợp ngày/tháng khớp trực tiếp
                if (repeatStartDate.getMonth() == date.getMonth() &&
                        repeatStartDate.getDayOfMonth() == date.getDayOfMonth()) {
                    return true;
                }

                // 2b. Xử lý trường hợp năm nhuận
                // (ví dụ: bắt đầu vào 29/02, sẽ lặp vào 28/02 vào năm không nhuận)
                if (repeatStartDate.getMonth() == java.time.Month.FEBRUARY &&
                        repeatStartDate.getDayOfMonth() == 29 &&
                        date.getMonth() == java.time.Month.FEBRUARY &&
                        date.getDayOfMonth() == 28 && !date.isLeapYear()) {
                    return true;
                }
                return false;

            case NONE:
            default:
                return false;
        }
    }
}