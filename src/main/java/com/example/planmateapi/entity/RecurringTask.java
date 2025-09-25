package com.example.planmateapi.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Set;

@Data
@Entity
@Table(name = "recurring_tasks")
@AllArgsConstructor
@NoArgsConstructor
public class RecurringTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(length = 50)
    private String timezone = "UTC";

    @Enumerated(EnumType.STRING)
    @Column(name = "repeat_type", nullable = false)
    private RepeatType repeatType;

    @Column(name = "repeat_interval")
    private Integer repeatInterval = 1;

    @Column(name = "repeat_days", columnDefinition = "TEXT")
    private String repeatDays;

    @Column(name = "repeat_day_of_month")
    private Integer repeatDayOfMonth;

    @Column(name = "repeat_week_of_month")
    private Integer repeatWeekOfMonth;

    @Column(name = "repeat_day_of_week")
    private Integer repeatDayOfWeek;

    @Column(name = "repeat_start", nullable = false)
    private LocalDate repeatStart;

    @Column(name = "repeat_end")
    private LocalDate repeatEnd;

    @Column(columnDefinition = "TEXT")
    private String exceptions;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "recurring_task_tags", // Tên bảng nối mới
            joinColumns = @JoinColumn(name = "recurring_task_id"), // Khóa ngoại trỏ đến bảng hiện tại
            inverseJoinColumns = @JoinColumn(name = "tag_id") // Khóa ngoại trỏ đến bảng tags
    )
    private Set<Tag> tags;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calendar_id", nullable = false)
    private Calendar calendar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
