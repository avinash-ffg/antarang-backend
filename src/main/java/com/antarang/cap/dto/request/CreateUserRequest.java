package com.antarang.cap.dto.request;

import com.antarang.cap.domain.enums.ScopeType;
import com.antarang.cap.domain.enums.UserType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateUserRequest(
        @NotBlank @Email String email,
        @NotBlank String password,
        @NotNull UserType userType,
        UUID primaryOrgUnitId,
        ScopeType scopeType,
        UUID scopeId
) {
}
