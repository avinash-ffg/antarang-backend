package com.antarang.cap.controller;

import com.antarang.cap.common.ApiResponse;
import com.antarang.cap.dto.request.CreateUserRequest;
import com.antarang.cap.dto.response.UserResponse;
import com.antarang.cap.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUser() {
        return ApiResponse.success(userService.getCurrentUser());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'SUB_ADMIN')")
    public ApiResponse<List<UserResponse>> listUsers() {
        return ApiResponse.success(userService.listUsers());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'SUB_ADMIN')")
    public ApiResponse<UserResponse> getUser(@PathVariable UUID id) {
        return ApiResponse.success(userService.getUserById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.success(userService.createUser(request), "User created successfully");
    }
}
