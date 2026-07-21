package com.antarang.cap.dto.response;

import java.util.UUID;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String role,
        UUID primaryOrgUnitId
) {
}
