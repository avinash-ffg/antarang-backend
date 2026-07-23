package com.antarang.cap.repository;

import com.antarang.cap.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    @Query("""
            SELECT DISTINCT u FROM User u
            LEFT JOIN FETCH u.userRoles ur
            LEFT JOIN FETCH ur.role
            WHERE u.email = :email AND u.isDeleted = false
            """)
    Optional<User> findByEmailWithRoles(@Param("email") String email);

    @Query("""
            SELECT DISTINCT u FROM User u
            LEFT JOIN FETCH u.userRoles ur
            LEFT JOIN FETCH ur.role
            WHERE u.id = :id AND u.isDeleted = false
            """)
    Optional<User> findByIdWithRoles(@Param("id") UUID id);

    List<User> findByTenantIdAndIsDeletedFalse(UUID tenantId);

    boolean existsByTenantIdAndEmailAndIsDeletedFalse(UUID tenantId, String email);
}
