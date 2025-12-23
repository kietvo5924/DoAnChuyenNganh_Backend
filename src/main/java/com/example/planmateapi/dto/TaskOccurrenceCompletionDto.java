package com.example.planmateapi.dto;

import com.example.planmateapi.entity.TaskType;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskOccurrenceCompletionDto {
    private Long calendarId;
    private Long taskId;
    private TaskType taskType;
    private LocalDate occurrenceDate;
    private boolean completed;
}
