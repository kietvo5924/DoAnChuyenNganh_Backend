package com.example.planmateapi.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "task_occurrence_completions")
@AllArgsConstructor
@NoArgsConstructor
public class TaskOccurrenceCompletion {

    @EmbeddedId
    private TaskOccurrenceCompletionId id;

    @Column(name = "calendar_id", nullable = false)
    private Long calendarId;

    @Column(name = "completed", nullable = false)
    private boolean completed = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public static TaskOccurrenceCompletion of(Long calendarId, Long taskId, TaskType taskType, LocalDate date) {
        TaskOccurrenceCompletion c = new TaskOccurrenceCompletion();
        c.setId(new TaskOccurrenceCompletionId(taskId, taskType, date));
        c.setCalendarId(calendarId);
        c.setCompleted(true);
        return c;
    }
}
