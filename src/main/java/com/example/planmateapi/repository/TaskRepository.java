package com.example.planmateapi.repository;

import com.example.planmateapi.entity.Tag;
import com.example.planmateapi.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    // Tìm tất cả các Task thuộc về một calendar_id cụ thể.
    List<Task> findByCalendarId(Long calendarId);

    List<Task> findByTagsContains(Tag tag);
}