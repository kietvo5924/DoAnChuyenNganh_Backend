package com.example.planmateapi.service;

import com.example.planmateapi.dto.AdminDashboardAnalytics;
import com.example.planmateapi.entity.ReportStatus;
import com.example.planmateapi.entity.User;
import com.example.planmateapi.repository.CalendarAbuseReportRepository;
import com.example.planmateapi.repository.RecurringTaskRepository;
import com.example.planmateapi.repository.TaskRepository;
import com.example.planmateapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();
    private static final int WEEK_WINDOW_DAYS = 6;
    private static final int MONTH_WINDOW_WEEKS = 4;

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final RecurringTaskRepository recurringTaskRepository;
    private final CalendarAbuseReportRepository calendarAbuseReportRepository;

    public AdminDashboardAnalytics buildDashboardAnalytics() {
        OffsetDateTime now = OffsetDateTime.now();
        LocalDate today = now.atZoneSameInstant(DEFAULT_ZONE).toLocalDate();

        LocalDate weekStartDate = today.minusDays(WEEK_WINDOW_DAYS);
        OffsetDateTime weekStartDateTime = weekStartDate.atStartOfDay(DEFAULT_ZONE).toOffsetDateTime();

        LocalDate monthStartDate = alignToMonday(today.minusWeeks(MONTH_WINDOW_WEEKS - 1));
        OffsetDateTime monthStartDateTime = monthStartDate.atStartOfDay(DEFAULT_ZONE).toOffsetDateTime();

        List<User> weeklyUsers = userRepository.findByCreatedAtBetween(weekStartDateTime, now);
        List<User> monthlyUsers = userRepository.findByCreatedAtBetween(monthStartDateTime, now);

        List<AdminDashboardAnalytics.DailyMetric> weeklyMetrics = buildDailyMetrics(weekStartDate, today, weeklyUsers);
        List<AdminDashboardAnalytics.WeeklyMetric> monthlyMetrics = buildWeeklyMetrics(monthStartDate, today,
                monthlyUsers);

        long weeklyNewUsers = weeklyMetrics.stream().mapToLong(AdminDashboardAnalytics.DailyMetric::getCount).sum();
        long monthlyNewUsers = monthlyMetrics.stream().mapToLong(AdminDashboardAnalytics.WeeklyMetric::getCount).sum();

        long tasksCreatedThisWeek = taskRepository.countByCreatedAtBetween(weekStartDateTime, now)
                + recurringTaskRepository.countByCreatedAtBetween(weekStartDateTime, now);
        long tasksCreatedThisMonth = taskRepository.countByCreatedAtBetween(monthStartDateTime, now)
                + recurringTaskRepository.countByCreatedAtBetween(monthStartDateTime, now);

        long completedTasks = taskRepository.countCompletedTasks(now);
        long overdueTasks = taskRepository.countOverdueTasks(now);

        List<AdminDashboardAnalytics.ReportStatusMetric> reportStatusMetrics = buildReportStatusMetrics();
        long pendingReports = reportStatusMetrics.stream()
                .filter(metric -> metric.getStatus() == ReportStatus.PENDING)
                .mapToLong(AdminDashboardAnalytics.ReportStatusMetric::getCount)
                .findFirst()
                .orElse(0L);

        return AdminDashboardAnalytics.builder()
                .weeklyUserRegistrations(weeklyMetrics)
                .monthlyUserRegistrations(monthlyMetrics)
                .weeklyNewUsers(weeklyNewUsers)
                .monthlyNewUsers(monthlyNewUsers)
                .tasksCreatedThisWeek(tasksCreatedThisWeek)
                .tasksCreatedThisMonth(tasksCreatedThisMonth)
                .completedTasks(completedTasks)
                .overdueTasks(overdueTasks)
                .pendingReports(pendingReports)
                .reportStatusMetrics(reportStatusMetrics)
                .build();
    }

    private List<AdminDashboardAnalytics.DailyMetric> buildDailyMetrics(LocalDate startDate, LocalDate endDate,
            List<User> users) {
        Map<LocalDate, Long> counts = new HashMap<>();
        for (User user : users) {
            LocalDate date = toLocalDate(user.getCreatedAt());
            if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
                counts.merge(date, 1L, Long::sum);
            }
        }

        List<AdminDashboardAnalytics.DailyMetric> result = new ArrayList<>();
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            result.add(new AdminDashboardAnalytics.DailyMetric(cursor, counts.getOrDefault(cursor, 0L)));
            cursor = cursor.plusDays(1);
        }
        return result;
    }

    private List<AdminDashboardAnalytics.WeeklyMetric> buildWeeklyMetrics(LocalDate startDate, LocalDate endDate,
            List<User> users) {
        Map<LocalDate, Long> counts = new HashMap<>();
        for (User user : users) {
            LocalDate date = toLocalDate(user.getCreatedAt());
            LocalDate bucketStart = alignToMonday(date);
            if (!bucketStart.isBefore(startDate) && !bucketStart.isAfter(endDate)) {
                counts.merge(bucketStart, 1L, Long::sum);
            }
        }

        List<AdminDashboardAnalytics.WeeklyMetric> result = new ArrayList<>();
        LocalDate cursor = alignToMonday(startDate);
        while (!cursor.isAfter(endDate)) {
            LocalDate bucketEnd = cursor.plusDays(WEEK_WINDOW_DAYS);
            long count = counts.getOrDefault(cursor, 0L);
            result.add(new AdminDashboardAnalytics.WeeklyMetric(cursor, bucketEnd, count));
            cursor = cursor.plusWeeks(1);
        }
        return result;
    }

    private List<AdminDashboardAnalytics.ReportStatusMetric> buildReportStatusMetrics() {
        return Arrays.stream(ReportStatus.values())
                .map(status -> new AdminDashboardAnalytics.ReportStatusMetric(status,
                        calendarAbuseReportRepository.countByStatus(status)))
                .collect(Collectors.toList());
    }

    private LocalDate alignToMonday(LocalDate date) {
        return date.with(DayOfWeek.MONDAY);
    }

    private LocalDate toLocalDate(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return LocalDate.now(DEFAULT_ZONE);
        }
        return dateTime.atZoneSameInstant(DEFAULT_ZONE).toLocalDate();
    }
}
