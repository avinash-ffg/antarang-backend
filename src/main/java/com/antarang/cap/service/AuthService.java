package com.antarang.cap.service;

import com.antarang.cap.domain.entity.AuthAttempt;
import com.antarang.cap.domain.entity.User;
import com.antarang.cap.domain.enums.AuthAttemptStatus;
import com.antarang.cap.domain.enums.UserStatus;
import com.antarang.cap.dto.request.LoginRequest;
import com.antarang.cap.dto.response.LoginResponse;
import com.antarang.cap.exception.BusinessException;
import com.antarang.cap.repository.AuthAttemptRepository;
import com.antarang.cap.repository.UserRepository;
import com.antarang.cap.security.JwtService;
import com.antarang.cap.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final AuthAttemptRepository authAttemptRepository;
    private final JwtService jwtService;

    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            AuthAttemptRepository authAttemptRepository,
            JwtService jwtService
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.authAttemptRepository = authAttemptRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );

            User user = userRepository.findByEmailWithRoles(request.email())
                    .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

            if (user.getStatus() != UserStatus.ACTIVE) {
                logAttempt(request.email(), user.getTenant().getId(), httpRequest, AuthAttemptStatus.FAILURE, "User inactive");
                throw new BusinessException("User account is not active", "USER_INACTIVE");
            }

            UserPrincipal principal = UserPrincipal.from(user);
            logAttempt(request.email(), user.getTenant().getId(), httpRequest, AuthAttemptStatus.SUCCESS, null);

            return new LoginResponse(
                    jwtService.generateAccessToken(principal),
                    jwtService.generateRefreshToken(principal),
                    principal.getPrimaryRole(),
                    principal.getPrimaryOrgUnitId()
            );
        } catch (BadCredentialsException ex) {
            logAttempt(request.email(), null, httpRequest, AuthAttemptStatus.FAILURE, "Invalid credentials");
            throw ex;
        }
    }

    private void logAttempt(
            String email,
            java.util.UUID tenantId,
            HttpServletRequest httpRequest,
            AuthAttemptStatus status,
            String failureReason
    ) {
        AuthAttempt attempt = new AuthAttempt();
        attempt.setEmail(email);
        attempt.setTenantId(tenantId);
        attempt.setIpAddress(httpRequest.getRemoteAddr());
        attempt.setUserAgent(httpRequest.getHeader("User-Agent"));
        attempt.setStatus(status);
        attempt.setFailureReason(failureReason);
        authAttemptRepository.save(attempt);
    }
}
