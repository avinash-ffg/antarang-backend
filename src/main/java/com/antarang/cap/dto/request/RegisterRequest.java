package com.antarang.cap.dto.request;

import com.antarang.cap.domain.enums.UserType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * FFG register payload with aliases: {@code tenantCode} accepted when {@code tenantId} is absent;
 * minimal clients may send only email/password/tenantCode.
 */
public record RegisterRequest(
        UUID tenantId,
        String tenantCode,
        String firstName,
        String lastName,
        @NotBlank @Email String email,
        String mobileNumber,
        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String password,
        UserType userType,
        LocalDate dateOfBirth,
        UUID primaryOrgUnitId,
        UUID preferredPlatformLanguageId,
        UUID preferredAssessmentLanguageId
) {
}
