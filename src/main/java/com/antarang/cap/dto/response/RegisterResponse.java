package com.antarang.cap.dto.response;

import com.antarang.cap.domain.enums.UserStatus;
import com.antarang.cap.domain.enums.UserType;

import java.util.UUID;

public record RegisterResponse(
        UUID userId,
        UserType userType,
        UserStatus status,
        boolean consentRequired,
        String consentType
) {
}
