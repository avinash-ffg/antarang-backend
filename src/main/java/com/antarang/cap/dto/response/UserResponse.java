package com.antarang.cap.dto.response;

import com.antarang.cap.domain.enums.UserStatus;
import com.antarang.cap.domain.enums.UserType;

import java.util.UUID;

public record UserResponse(
        UUID id,
        UUID tenantId,
        String email,
        UserType userType,
        UserStatus status,
        UUID primaryOrgUnitId,
        String role
) {
}
