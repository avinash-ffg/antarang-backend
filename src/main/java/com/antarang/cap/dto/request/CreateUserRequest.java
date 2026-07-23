package com.antarang.cap.dto.request;

import com.antarang.cap.domain.enums.ScopeType;
import com.antarang.cap.domain.enums.UserType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateUserRequest(
        @NotBlank @Email String email,
        @NotBlank String password,
        @NotNull UserType userType,
        String firstName,
        String lastName,
        String mobileNumber,
        LocalDate dateOfBirth,
        UUID primaryOrgUnitId,
        UUID preferredPlatformLanguageId,
        UUID preferredAssessmentLanguageId,
        ScopeType scopeType,
        UUID scopeId,
        List<UUID> roleIds
) {
}
