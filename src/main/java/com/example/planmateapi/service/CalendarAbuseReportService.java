package com.example.planmateapi.service;

import com.example.planmateapi.dto.CalendarAbuseReportDto;
import com.example.planmateapi.dto.CalendarReportRequestDto;
import com.example.planmateapi.entity.*;
import com.example.planmateapi.repository.CalendarAbuseReportRepository;
import com.example.planmateapi.repository.CalendarShareRepository;
import com.example.planmateapi.repository.TaskRepository;
import com.example.planmateapi.repository.RecurringTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalendarAbuseReportService {

    private final CalendarShareRepository calendarShareRepository;
    private final CalendarAbuseReportRepository reportRepository;
    private final AuthenticationService authenticationService;
    private final TaskRepository taskRepository;
    private final RecurringTaskRepository recurringTaskRepository;
    private final AdminService adminService;

    @Transactional
    public void submitReport(CalendarReportRequestDto request) {
        User reporter = authenticationService.getCurrentAuthenticatedUser();
        CalendarShare share = calendarShareRepository
                .findByCalendarIdAndSharedWithUserId(request.getCalendarId(), reporter.getId())
                .orElseThrow(() -> new IllegalStateException("Bạn không được chia sẻ lịch này."));

        if (share.getCalendar().getOwner().getId().equals(reporter.getId())) {
            throw new IllegalStateException("Không thể báo cáo lịch của chính bạn.");
        }

        boolean alreadyReported = reportRepository.existsByShare_IdAndStatusIn(
                share.getId(), Set.of(ReportStatus.PENDING));
        if (alreadyReported) {
            throw new IllegalStateException("Bạn đã gửi báo cáo cho lịch này, vui lòng chờ Admin xử lý.");
        }

        CalendarAbuseReport report = new CalendarAbuseReport();
        report.setCalendar(share.getCalendar());
        report.setShare(share);
        report.setReportedUser(share.getCalendar().getOwner());
        report.setReporter(reporter);
        report.setReason(request.getReason());
        report.setDescription(request.getDescription());
        report.setStatus(ReportStatus.PENDING);
        reportRepository.save(report);
    }

    @Transactional(readOnly = true)
    public List<CalendarAbuseReportDto> getAllReports() {
        return reportRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(report -> mapToDto(report, false))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CalendarAbuseReportDto getReportDetail(Long reportId) {
        CalendarAbuseReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy báo cáo."));
        return mapToDto(report, true);
    }

    @Transactional
    public void warnViolator(Long reportId, String adminNote) {
        CalendarAbuseReport report = setBaseStatus(reportId, ReportStatus.WARNED, adminNote);
        reportRepository.save(report);
    }

    @Transactional
    public void dismissReport(Long reportId, String adminNote) {
        CalendarAbuseReport report = setBaseStatus(reportId, ReportStatus.DISMISSED, adminNote);
        reportRepository.save(report);
    }

    @Transactional
    public void lockViolator(Long reportId, String adminNote) {
        CalendarAbuseReport report = setBaseStatus(reportId, ReportStatus.ACTION_TAKEN, adminNote);
        adminService.lockOrUnlockUser(report.getReportedUser().getId(), true);
        reportRepository.save(report);
    }

    private CalendarAbuseReport setBaseStatus(Long reportId, ReportStatus status, String adminNote) {
        CalendarAbuseReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy báo cáo."));
        report.setStatus(status);
        report.setAdminNote(adminNote);
        report.setHandledBy(authenticationService.getCurrentAuthenticatedUser());
        return report;
    }

    private CalendarAbuseReportDto mapToDto(CalendarAbuseReport report, boolean includeTasks) {
        List<CalendarAbuseReportDto.TaskSnapshot> taskSnapshots = includeTasks
                ? buildTaskSnapshots(report.getCalendar())
                : List.of();

        User reporter = report.getReporter();
        User reportedUser = report.getReportedUser();
        Calendar calendar = report.getCalendar();

        return CalendarAbuseReportDto.builder()
                .id(report.getId())
                .calendarId(calendar.getId())
                .calendarName(calendar.getName())
                .calendarDescription(calendar.getDescription())
                .reportedUserId(reportedUser.getId())
                .reportedUserName(reportedUser.getFullName())
                .reportedUserEmail(reportedUser.getEmail())
                .reporterId(reporter.getId())
                .reporterName(reporter.getFullName())
                .reporterEmail(reporter.getEmail())
                .reason(report.getReason())
                .description(report.getDescription())
                .status(report.getStatus())
                .adminNote(report.getAdminNote())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .tasks(taskSnapshots)
                .build();
    }

    private List<CalendarAbuseReportDto.TaskSnapshot> buildTaskSnapshots(Calendar calendar) {
        List<CalendarAbuseReportDto.TaskSnapshot> snapshots = taskRepository
                .findByCalendarId(calendar.getId()).stream()
                .map(task -> CalendarAbuseReportDto.TaskSnapshot.builder()
                        .id(task.getId())
                        .title(task.getTitle())
                        .startTime(task.getStartTime())
                        .endTime(task.getEndTime())
                        .type(CalendarAbuseReportDto.TaskSnapshot.SnapshotType.SINGLE)
                        .build())
                .collect(Collectors.toList());

        snapshots.addAll(recurringTaskRepository.findByCalendarId(calendar.getId()).stream()
                .map(this::mapRecurringTask)
                .collect(Collectors.toList()));

        return snapshots.stream()
                .sorted((a, b) -> {
                    if (a.getStartTime() == null && b.getStartTime() == null) {
                        return 0;
                    }
                    if (a.getStartTime() == null) {
                        return 1;
                    }
                    if (b.getStartTime() == null) {
                        return -1;
                    }
                    return a.getStartTime().compareTo(b.getStartTime());
                })
                .limit(50)
                .collect(Collectors.toList());
    }

    private CalendarAbuseReportDto.TaskSnapshot mapRecurringTask(RecurringTask recurringTask) {
        OffsetDateTime start = toOffsetDateTime(recurringTask.getRepeatStart(), recurringTask.getStartTime(),
                recurringTask.getTimezone());
        LocalDate endDate = recurringTask.getRepeatEnd() != null ? recurringTask.getRepeatEnd()
                : recurringTask.getRepeatStart();
        OffsetDateTime end = toOffsetDateTime(endDate, recurringTask.getEndTime(), recurringTask.getTimezone());
        return CalendarAbuseReportDto.TaskSnapshot.builder()
                .id(recurringTask.getId())
                .title(recurringTask.getTitle())
                .startTime(start)
                .endTime(end)
                .type(CalendarAbuseReportDto.TaskSnapshot.SnapshotType.RECURRING)
                .build();
    }

    private OffsetDateTime toOffsetDateTime(LocalDate date, LocalTime time, String timezone) {
        if (date == null || time == null) {
            return null;
        }
        ZoneId zoneId;
        try {
            zoneId = (timezone != null && !timezone.isBlank()) ? ZoneId.of(timezone) : ZoneId.systemDefault();
        } catch (Exception ex) {
            zoneId = ZoneId.systemDefault();
        }
        return date.atTime(time).atZone(zoneId).toOffsetDateTime();
    }
}
