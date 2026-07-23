package com.antarang.cap.dto.response;

import com.antarang.cap.domain.enums.UserStatus;
import com.antarang.cap.domain.enums.UserType;

import java.util.List;
import java.util.UUID;

public record AuthUserSummary(
        UUID id,
        String firstName,
        String lastName,
        String email,
        UserType userType,
        UserStatus status,
        List<String> roles,
        UUID tenantId,
        UUID primaryOrgUnitId,
        String preferredPlatformLanguageCode,
        String preferredAssessmentLanguageCode
) {
}
