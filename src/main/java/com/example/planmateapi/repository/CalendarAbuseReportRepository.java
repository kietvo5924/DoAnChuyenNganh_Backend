package com.example.planmateapi.repository;

import com.example.planmateapi.entity.CalendarAbuseReport;
import com.example.planmateapi.entity.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CalendarAbuseReportRepository extends JpaRepository<CalendarAbuseReport, Long> {

    List<CalendarAbuseReport> findAllByOrderByCreatedAtDesc();

    List<CalendarAbuseReport> findByStatusOrderByCreatedAtDesc(ReportStatus status);

    boolean existsByShare_IdAndStatusIn(Long shareId, Collection<ReportStatus> statuses);

    long countByStatus(ReportStatus status);
}
