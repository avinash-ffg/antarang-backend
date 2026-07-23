package com.antarang.cap.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Self-service profile update. All fields are optional; only non-null values are applied.
 */
public record UpdateProfileRequest(
        @Email String email,
        @Size(min = 8, message = "Password must be at least 8 characters") String password,
        String firstName,
        String lastName,
        String mobileNumber,
        LocalDate dateOfBirth,
        UUID preferredPlatformLanguageId,
        UUID preferredAssessmentLanguageId
) {
}
