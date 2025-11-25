package com.example.planmateapi.repository;

import com.example.planmateapi.entity.Tag;
import com.example.planmateapi.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    // Tìm tất cả các Task thuộc về một calendar_id cụ thể.
    List<Task> findByCalendarId(Long calendarId);

    List<Task> findByTagsContains(Tag tag);

    // Tìm task thường theo lịch và tên (tương đối)
    List<Task> findByCalendarIdAndTitleContainingIgnoreCase(Long calendarId, String title);

    List<Task> findByCreatedAtBetween(OffsetDateTime start, OffsetDateTime end);

    long countByCreatedAtBetween(OffsetDateTime start, OffsetDateTime end);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.endTime < :now AND t.updatedAt > t.endTime")
    long countCompletedTasks(@Param("now") OffsetDateTime now);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.endTime < :now AND (t.updatedAt IS NULL OR t.updatedAt <= t.endTime)")
    long countOverdueTasks(@Param("now") OffsetDateTime now);
}