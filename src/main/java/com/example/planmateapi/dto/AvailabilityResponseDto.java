package com.example.planmateapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor // Cần thiết cho Spring
@AllArgsConstructor // Cần thiết cho Builder
public class AvailabilityResponseDto {
    private boolean isFree;
    private String message;
    private List<ConflictInfo> conflicts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConflictInfo {
        private String taskTitle;
        private String calendarName;
        private OffsetDateTime conflictStartTime;
        private OffsetDateTime conflictEndTime;
    }
}