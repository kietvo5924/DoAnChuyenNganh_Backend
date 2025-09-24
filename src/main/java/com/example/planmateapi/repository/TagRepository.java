package com.example.planmateapi.repository;

import com.example.planmateapi.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

public interface TagRepository extends JpaRepository<Tag, Long> {
    List<Tag> findByUserId(Long userId);

    // Tìm các tag theo danh sách ID
    Set<Tag> findByIdIn(Set<Long> ids);
    boolean existsByNameAndUserId(String name, Long userId);
}
