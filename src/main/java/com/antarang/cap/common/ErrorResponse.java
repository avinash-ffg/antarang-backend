package com.antarang.cap.common;

import java.time.Instant;

public record ErrorResponse(
        boolean success,
        String message,
        ErrorDetail error,
        Instant timestamp
) {

    public static ErrorResponse of(String message, String code) {
        return of(message, code, null);
    }

    public static ErrorResponse of(String message, String code, Object details) {
        return new ErrorResponse(false, message, new ErrorDetail(code, details), Instant.now());
    }
}
