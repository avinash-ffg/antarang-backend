package com.antarang.cap.dto.response;

import com.antarang.cap.domain.enums.UserStatus;
import com.antarang.cap.domain.enums.UserType;

import java.util.List;
import java.util.UUID;

public record AuthMeResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String mobileNumber,
        UserType userType,
        UserStatus status,
        List<String> roles,
        List<String> permissions,
        UUID tenantId,
        UUID primaryOrgUnitId,
        UUID preferredPlatformLanguageId,
        UUID preferredAssessmentLanguageId
) {
}
