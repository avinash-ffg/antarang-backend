package com.antarang.cap.repository;

import com.antarang.cap.domain.entity.AuthAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuthAttemptRepository extends JpaRepository<AuthAttempt, UUID> {
}
