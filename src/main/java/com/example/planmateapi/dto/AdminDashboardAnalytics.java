package com.example.planmateapi.dto;

import com.example.planmateapi.entity.ReportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class AdminDashboardAnalytics {

    private final List<DailyMetric> weeklyUserRegistrations;
    private final List<WeeklyMetric> monthlyUserRegistrations;
    private final long weeklyNewUsers;
    private final long monthlyNewUsers;
    private final long tasksCreatedThisWeek;
    private final long tasksCreatedThisMonth;
    private final long completedTasks;
    private final long overdueTasks;
    private final long pendingReports;
    private final List<ReportStatusMetric> reportStatusMetrics;

    @Getter
    @AllArgsConstructor
    public static class DailyMetric {
        private final LocalDate date;
        private final long count;
    }

    @Getter
    @AllArgsConstructor
    public static class WeeklyMetric {
        private final LocalDate start;
        private final LocalDate end;
        private final long count;
    }

    @Getter
    @AllArgsConstructor
    public static class ReportStatusMetric {
        private final ReportStatus status;
        private final long count;
    }
}
