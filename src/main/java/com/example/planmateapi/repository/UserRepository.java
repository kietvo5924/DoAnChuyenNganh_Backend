package com.example.planmateapi.repository;

import com.example.planmateapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    List<User> findByCreatedAtBetween(OffsetDateTime start, OffsetDateTime end);
}
