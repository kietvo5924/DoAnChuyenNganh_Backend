package com.example.planmateapi.repository;

import com.example.planmateapi.entity.TaskOccurrenceCompletion;
import com.example.planmateapi.entity.TaskOccurrenceCompletionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TaskOccurrenceCompletionRepository extends JpaRepository<TaskOccurrenceCompletion, TaskOccurrenceCompletionId> {
    List<TaskOccurrenceCompletion> findByCalendarIdAndIdOccurrenceDateBetween(Long calendarId, LocalDate from, LocalDate to);
}
