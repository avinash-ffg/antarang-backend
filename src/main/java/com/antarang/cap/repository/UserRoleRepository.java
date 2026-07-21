package com.antarang.cap.repository;

import com.antarang.cap.domain.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

    List<UserRole> findByUserIdAndIsDeletedFalse(UUID userId);
}
