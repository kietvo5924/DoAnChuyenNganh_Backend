package com.example.planmateapi.repository;

import com.example.planmateapi.entity.CalendarShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CalendarShareRepository extends JpaRepository<CalendarShare, Long> {
    // Tìm các chia sẻ dựa trên người được chia sẻ
    List<CalendarShare> findBySharedWithUserId(Long userId);

    // Tìm các chia sẻ dựa trên lịch được chia sẻ
    List<CalendarShare> findByCalendarId(Long calendarId);

    // Tìm một chia sẻ cụ thể
    Optional<CalendarShare> findByCalendarIdAndSharedWithUserId(Long calendarId, Long userId);

    // Xóa một chia sẻ cụ thể
    void deleteByCalendarIdAndSharedWithUserId(Long calendarId, Long userId);
}
