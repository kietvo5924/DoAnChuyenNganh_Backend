package com.example.planmateapi.repository;

import com.example.planmateapi.entity.RecurringTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecurringTaskRepository extends JpaRepository<RecurringTask, Long> {
    // Tìm tất cả các RecurringTask thuộc về một calendar_id cụ thể.
    List<RecurringTask> findByCalendarId(Long calendarId);
}