package com.example.planmateapi.dto;

import com.example.planmateapi.entity.ReportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class CalendarAbuseReportDto {

    private final Long id;
    private final Long calendarId;
    private final String calendarName;
    private final String calendarDescription;
    private final Long reportedUserId;
    private final String reportedUserName;
    private final String reportedUserEmail;
    private final Long reporterId;
    private final String reporterName;
    private final String reporterEmail;
    private final String reason;
    private final String description;
    private final ReportStatus status;
    private final String adminNote;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;
    private final List<TaskSnapshot> tasks;

    public List<TaskSnapshot> getTasks() {
        return tasks == null ? Collections.emptyList() : tasks;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class TaskSnapshot {
        private final Long id;
        private final String title;
        private final OffsetDateTime startTime;
        private final OffsetDateTime endTime;
        private final SnapshotType type;

        public enum SnapshotType {
            SINGLE,
            RECURRING
        }
    }
}
