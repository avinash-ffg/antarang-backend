package com.antarang.cap.service;

import com.antarang.cap.config.JwtProperties;
import com.antarang.cap.domain.entity.AuthAttempt;
import com.antarang.cap.domain.entity.Language;
import com.antarang.cap.domain.entity.OrgUnit;
import com.antarang.cap.domain.entity.Permission;
import com.antarang.cap.domain.entity.RefreshToken;
import com.antarang.cap.domain.entity.Role;
import com.antarang.cap.domain.entity.Tenant;
import com.antarang.cap.domain.entity.User;
import com.antarang.cap.domain.entity.UserRole;
import com.antarang.cap.domain.entity.VerificationToken;
import com.antarang.cap.domain.enums.AuthAttemptStatus;
import com.antarang.cap.domain.enums.RoleName;
import com.antarang.cap.domain.enums.ScopeType;
import com.antarang.cap.domain.enums.UserStatus;
import com.antarang.cap.domain.enums.UserType;
import com.antarang.cap.domain.enums.VerificationTokenType;
import com.antarang.cap.dto.request.ForgotPasswordRequest;
import com.antarang.cap.dto.request.LoginRequest;
import com.antarang.cap.dto.request.RefreshTokenRequest;
import com.antarang.cap.dto.request.RegisterRequest;
import com.antarang.cap.dto.request.ResetPasswordRequest;
import com.antarang.cap.dto.response.AuthMeResponse;
import com.antarang.cap.dto.response.AuthUserSummary;
import com.antarang.cap.dto.response.LoginResponse;
import com.antarang.cap.dto.response.RegisterResponse;
import com.antarang.cap.exception.BusinessException;
import com.antarang.cap.exception.ResourceNotFoundException;
import com.antarang.cap.repository.AuthAttemptRepository;
import com.antarang.cap.repository.LanguageRepository;
import com.antarang.cap.repository.OrgUnitRepository;
import com.antarang.cap.repository.RefreshTokenRepository;
import com.antarang.cap.repository.RoleRepository;
import com.antarang.cap.repository.TenantRepository;
import com.antarang.cap.repository.UserRepository;
import com.antarang.cap.repository.VerificationTokenRepository;
import com.antarang.cap.security.JwtService;
import com.antarang.cap.security.TokenDenylistService;
import com.antarang.cap.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final Duration PASSWORD_RESET_TOKEN_TTL = Duration.ofHours(1);
    private static final int RESET_TOKEN_BYTES = 32;

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final AuthAttemptRepository authAttemptRepository;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantRepository tenantRepository;
    private final RoleRepository roleRepository;
    private final TokenDenylistService tokenDenylistService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final LanguageRepository languageRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            AuthAttemptRepository authAttemptRepository,
            JwtService jwtService,
            JwtProperties jwtProperties,
            VerificationTokenRepository verificationTokenRepository,
            PasswordEncoder passwordEncoder,
            TenantRepository tenantRepository,
            RoleRepository roleRepository,
            TokenDenylistService tokenDenylistService,
            RefreshTokenRepository refreshTokenRepository,
            OrgUnitRepository orgUnitRepository,
            LanguageRepository languageRepository
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.authAttemptRepository = authAttemptRepository;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tenantRepository = tenantRepository;
        this.roleRepository = roleRepository;
        this.tokenDenylistService = tokenDenylistService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.languageRepository = languageRepository;
    }

    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String loginId = request.resolvedLoginId();
        if (loginId == null || loginId.isBlank() || request.password() == null || request.password().isBlank()) {
            throw new BusinessException("loginId/email and password are required", "VALIDATION_ERROR");
        }
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginId, request.password())
            );

            User user = userRepository.findByEmailWithRoles(loginId)
                    .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

            if (user.getStatus() != UserStatus.ACTIVE) {
                logAttempt(loginId, user.getTenant().getId(), httpRequest, AuthAttemptStatus.FAILURE, "User inactive");
                throw new BusinessException("User account is not active", "USER_INACTIVE");
            }

            user.setLastLoginAt(Instant.now());
            userRepository.save(user);

            UserPrincipal principal = UserPrincipal.from(user);
            logAttempt(loginId, user.getTenant().getId(), httpRequest, AuthAttemptStatus.SUCCESS, null);
            return issueTokens(user, principal);
        } catch (BadCredentialsException ex) {
            logAttempt(loginId, null, httpRequest, AuthAttemptStatus.FAILURE, "Invalid credentials");
            throw ex;
        }
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        Tenant tenant = resolveTenant(request.tenantId(), request.tenantCode());

        if (userRepository.existsByTenantIdAndEmailAndIsDeletedFalse(tenant.getId(), request.email())) {
            throw new BusinessException("Email is already registered", "DUPLICATE_RESOURCE");
        }

        UserType userType = request.userType() != null ? request.userType() : UserType.STUDENT;
        Role role = roleRepository.findByName(RoleName.valueOf(userType.name()))
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        User user = new User();
        user.setTenant(tenant);
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setUserType(userType);
        user.setStatus(UserStatus.PENDING);
        user.setFirstName(request.firstName() != null ? request.firstName() : request.email().split("@")[0]);
        user.setLastName(request.lastName());
        user.setMobileNumber(request.mobileNumber());
        user.setDateOfBirth(request.dateOfBirth());
        user.setPreferredPlatformLanguageId(request.preferredPlatformLanguageId());
        user.setPreferredAssessmentLanguageId(request.preferredAssessmentLanguageId());

        if (request.primaryOrgUnitId() != null) {
            OrgUnit orgUnit = orgUnitRepository.findById(request.primaryOrgUnitId())
                    .filter(o -> !o.isDeleted())
                    .orElseThrow(() -> new ResourceNotFoundException("Org unit not found"));
            if (!orgUnit.getTenant().getId().equals(tenant.getId())) {
                throw new BusinessException("Cross-tenant access is not allowed", "ACCESS_DENIED");
            }
            user.setPrimaryOrgUnit(orgUnit);
        }

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        userRole.setScopeType(ScopeType.GLOBAL);
        user.getUserRoles().add(userRole);

        User saved = userRepository.save(user);
        logAttempt(request.email(), tenant.getId(), httpRequest, AuthAttemptStatus.SUCCESS, "Registration");

        boolean consentRequired = requiresGuardianConsent(request.dateOfBirth());
        return new RegisterResponse(
                saved.getId(),
                saved.getUserType(),
                saved.getStatus(),
                consentRequired,
                consentRequired ? "GUARDIAN" : null
        );
    }

    @Transactional(readOnly = true)
    public AuthMeResponse me() {
        UserPrincipal principal = currentPrincipal();
        User user = userRepository.findByIdWithRoles(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toMeResponse(user);
    }

    public void logout(String accessToken, String refreshToken) {
        if (accessToken != null && jwtService.isTokenValid(accessToken)) {
            tokenDenylistService.revoke(jwtService.extractJti(accessToken), jwtService.extractExpiration(accessToken));
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            revokeRefreshToken(refreshToken);
            if (jwtService.isTokenValid(refreshToken)) {
                tokenDenylistService.revoke(jwtService.extractJti(refreshToken), jwtService.extractExpiration(refreshToken));
            }
        }
    }

    @Transactional
    public LoginResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();

        if (!jwtService.isTokenValid(refreshToken) || !"refresh".equals(jwtService.extractTokenType(refreshToken))) {
            throw new BusinessException("Invalid or expired refresh token", "AUTH_TOKEN_INVALID");
        }

        String hash = hashToken(refreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BusinessException("Invalid or expired refresh token", "AUTH_TOKEN_INVALID"));

        if (stored.getRevokedAt() != null || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("Refresh token is no longer valid", "AUTH_TOKEN_INVALID");
        }

        if (tokenDenylistService.isRevoked(jwtService.extractJti(refreshToken))) {
            throw new BusinessException("Refresh token is no longer valid", "AUTH_TOKEN_INVALID");
        }

        User user = userRepository.findByIdWithRoles(stored.getUser().getId())
                .orElseThrow(() -> new BusinessException("Invalid or expired refresh token", "AUTH_TOKEN_INVALID"));

        if (user.getStatus() != UserStatus.ACTIVE && user.getStatus() != UserStatus.PENDING) {
            throw new BusinessException("User account is not active", "USER_INACTIVE");
        }

        stored.setRevokedAt(Instant.now());
        refreshTokenRepository.save(stored);
        tokenDenylistService.revoke(jwtService.extractJti(refreshToken), jwtService.extractExpiration(refreshToken));

        return issueTokens(user, UserPrincipal.from(user));
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request, HttpServletRequest httpRequest) {
        Optional<User> maybeUser = userRepository.findByEmailWithRoles(request.email());
        if (maybeUser.isEmpty()) {
            log.info("Password reset requested for unknown email '{}'; no token issued.", request.email());
            return;
        }

        User user = maybeUser.get();
        String rawToken = generateSecureToken();

        VerificationToken token = new VerificationToken();
        token.setUser(user);
        token.setTokenHash(hashToken(rawToken));
        token.setTokenType(VerificationTokenType.PASSWORD_RESET);
        token.setExpiresAt(Instant.now().plus(PASSWORD_RESET_TOKEN_TTL));
        token.setIpAddress(httpRequest.getRemoteAddr());
        verificationTokenRepository.save(token);

        log.info("[SIMULATED EMAIL] To: {} | Subject: Password Reset | Reset token: {} (valid for {} minutes)",
                user.getEmail(), rawToken, PASSWORD_RESET_TOKEN_TTL.toMinutes());
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        VerificationToken token = verificationTokenRepository
                .findByTokenHashAndTokenType(hashToken(request.token()), VerificationTokenType.PASSWORD_RESET)
                .orElseThrow(() -> new BusinessException("Invalid or expired reset token", "INVALID_TOKEN"));

        if (token.getUsedAt() != null) {
            throw new BusinessException("Reset token has already been used", "TOKEN_ALREADY_USED");
        }
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("Invalid or expired reset token", "INVALID_TOKEN");
        }

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        token.setUsedAt(Instant.now());
        verificationTokenRepository.save(token);
        log.info("Password successfully reset for user '{}'.", user.getEmail());
    }

    private LoginResponse issueTokens(User user, UserPrincipal principal) {
        String access = jwtService.generateAccessToken(principal);
        String refresh = jwtService.generateRefreshToken(principal);

        RefreshToken entity = new RefreshToken();
        entity.setUser(user);
        entity.setTokenHash(hashToken(refresh));
        entity.setExpiresAt(Instant.now().plusMillis(jwtProperties.getRefreshTokenExpirationMs()));
        refreshTokenRepository.save(entity);

        AuthUserSummary summary = toUserSummary(user);
        return new LoginResponse(
                access,
                refresh,
                jwtProperties.getAccessTokenExpirationMs() / 1000,
                summary,
                principal.getPrimaryRole(),
                principal.getPrimaryOrgUnitId()
        );
    }

    private void revokeRefreshToken(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(hashToken(rawRefreshToken)).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(Instant.now());
                refreshTokenRepository.save(token);
            }
        });
    }

    private Tenant resolveTenant(UUID tenantId, String tenantCode) {
        if (tenantId != null) {
            return tenantRepository.findById(tenantId)
                    .filter(t -> !t.isDeleted())
                    .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
        }
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new BusinessException("tenantId or tenantCode is required", "VALIDATION_ERROR");
        }
        return tenantRepository.findByCodeAndIsDeletedFalse(tenantCode)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
    }

    private boolean requiresGuardianConsent(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            return false;
        }
        return Period.between(dateOfBirth, LocalDate.now()).getYears() < 18;
    }

    private AuthUserSummary toUserSummary(User user) {
        List<String> roles = user.getUserRoles().stream()
                .filter(ur -> ur.isActive() && !ur.isDeleted())
                .map(ur -> ur.getRole().getName().name())
                .distinct()
                .toList();
        return new AuthUserSummary(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getUserType(),
                user.getStatus(),
                roles,
                user.getTenant().getId(),
                user.getPrimaryOrgUnit() != null ? user.getPrimaryOrgUnit().getId() : null,
                languageCode(user.getPreferredPlatformLanguageId()),
                languageCode(user.getPreferredAssessmentLanguageId())
        );
    }

    private AuthMeResponse toMeResponse(User user) {
        List<String> roles = user.getUserRoles().stream()
                .filter(ur -> ur.isActive() && !ur.isDeleted())
                .map(ur -> ur.getRole().getName().name())
                .distinct()
                .toList();
        List<String> permissions = user.getUserRoles().stream()
                .filter(ur -> ur.isActive() && !ur.isDeleted())
                .flatMap(ur -> ur.getRole().getPermissions().stream())
                .map(Permission::getCode)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        return new AuthMeResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getMobileNumber(),
                user.getUserType(),
                user.getStatus(),
                roles,
                permissions,
                user.getTenant().getId(),
                user.getPrimaryOrgUnit() != null ? user.getPrimaryOrgUnit().getId() : null,
                user.getPreferredPlatformLanguageId(),
                user.getPreferredAssessmentLanguageId()
        );
    }

    private String languageCode(UUID languageId) {
        if (languageId == null) {
            return null;
        }
        return languageRepository.findById(languageId).map(Language::getCode).orElse(null);
    }

    private UserPrincipal currentPrincipal() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new BusinessException("Authentication required", "UNAUTHORIZED");
        }
        return principal;
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[RESET_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm not available", ex);
        }
    }

    private void logAttempt(
            String email,
            UUID tenantId,
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
