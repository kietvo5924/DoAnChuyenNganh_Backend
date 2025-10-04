package com.example.planmateapi.repository;

import com.example.planmateapi.entity.Calendar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CalendarRepository extends JpaRepository<Calendar, Long> {
    // Tìm tất cả các Calendar thuộc về một owner_id cụ thể.
    List<Calendar> findByOwnerId(Long ownerId);
    long countByOwnerId(Long ownerId);
}