package com.antarang.cap.controller;

import com.antarang.cap.common.ApiResponse;
import com.antarang.cap.dto.request.ForgotPasswordRequest;
import com.antarang.cap.dto.request.LoginRequest;
import com.antarang.cap.dto.request.LogoutRequest;
import com.antarang.cap.dto.request.RefreshTokenRequest;
import com.antarang.cap.dto.request.RegisterRequest;
import com.antarang.cap.dto.request.ResetPasswordRequest;
import com.antarang.cap.dto.response.AuthMeResponse;
import com.antarang.cap.dto.response.LoginResponse;
import com.antarang.cap.dto.response.RegisterResponse;
import com.antarang.cap.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.success(authService.login(request, httpRequest), "Login successful");
    }

    @PostMapping("/register")
    public ApiResponse<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.success(authService.register(request, httpRequest), "User registered successfully");
    }

    @PostMapping({"/refresh", "/refresh-token"})
    public ApiResponse<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(authService.refresh(request), "Token refreshed successfully");
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestBody(required = false) LogoutRequest request,
            HttpServletRequest httpRequest
    ) {
        String header = httpRequest.getHeader(HttpHeaders.AUTHORIZATION);
        String accessToken = (header != null && header.startsWith("Bearer ")) ? header.substring(7) : null;
        String refreshToken = request != null ? request.refreshToken() : null;
        authService.logout(accessToken, refreshToken);
        return ApiResponse.success(null, "Logged out successfully");
    }

    @GetMapping("/me")
    public ApiResponse<AuthMeResponse> me() {
        return ApiResponse.success(authService.me(), "User profile fetched successfully");
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        authService.forgotPassword(request, httpRequest);
        return ApiResponse.success(null,
                "If an account exists for that email, a password reset link has been sent");
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.success(null, "Password has been reset successfully");
    }
}
