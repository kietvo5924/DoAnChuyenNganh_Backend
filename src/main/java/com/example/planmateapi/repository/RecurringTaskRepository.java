package com.example.planmateapi.repository;

import com.example.planmateapi.entity.RecurringTask;
import com.example.planmateapi.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecurringTaskRepository extends JpaRepository<RecurringTask, Long> {
    // Tìm tất cả các RecurringTask thuộc về một calendar_id cụ thể.
    List<RecurringTask> findByCalendarId(Long calendarId);

    List<RecurringTask> findByTagsContains(Tag tag);

    // Tìm task lặp lại theo lịch và tên (tương đối)
    List<RecurringTask> findByCalendarIdAndTitleContainingIgnoreCase(Long calendarId, String title);
}