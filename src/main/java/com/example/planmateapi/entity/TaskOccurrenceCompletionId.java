package com.example.planmateapi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TaskOccurrenceCompletionId implements Serializable {

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 20)
    private TaskType taskType;

    @Column(name = "occurrence_date", nullable = false)
    private LocalDate occurrenceDate;
}
