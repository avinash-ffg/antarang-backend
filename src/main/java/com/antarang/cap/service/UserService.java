package com.antarang.cap.service;

import com.antarang.cap.domain.entity.OrgUnit;
import com.antarang.cap.domain.entity.Role;
import com.antarang.cap.domain.entity.User;
import com.antarang.cap.domain.entity.UserRole;
import com.antarang.cap.domain.enums.RoleName;
import com.antarang.cap.domain.enums.ScopeType;
import com.antarang.cap.domain.enums.UserStatus;
import com.antarang.cap.dto.request.CreateUserRequest;
import com.antarang.cap.dto.response.UserResponse;
import com.antarang.cap.exception.BusinessException;
import com.antarang.cap.exception.ResourceNotFoundException;
import com.antarang.cap.repository.OrgUnitRepository;
import com.antarang.cap.repository.RoleRepository;
import com.antarang.cap.repository.UserRepository;
import com.antarang.cap.security.UserPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            OrgUnitRepository orgUnitRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        return toResponse(getCurrentPrincipalUser());
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        UserPrincipal principal = getCurrentPrincipal();
        User user = userRepository.findByIdWithRoles(id)
                .filter(found -> !found.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.getTenant().getId().equals(principal.getTenantId())) {
            throw new BusinessException("Cross-tenant access is not allowed", "ACCESS_DENIED");
        }

        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        UserPrincipal principal = getCurrentPrincipal();
        return userRepository.findByTenantIdAndIsDeletedFalse(principal.getTenantId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        UserPrincipal principal = getCurrentPrincipal();
        User creator = getCurrentPrincipalUser();

        if (userRepository.existsByTenantIdAndEmailAndIsDeletedFalse(principal.getTenantId(), request.email())) {
            throw new BusinessException("Email is already registered", "EMAIL_ALREADY_EXISTS");
        }

        Role role = roleRepository.findByName(RoleName.valueOf(request.userType().name()))
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        User user = new User();
        user.setTenant(creator.getTenant());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setUserType(request.userType());
        user.setStatus(UserStatus.ACTIVE);

        if (request.primaryOrgUnitId() != null) {
            OrgUnit orgUnit = orgUnitRepository.findById(request.primaryOrgUnitId())
                    .filter(found -> !found.isDeleted())
                    .orElseThrow(() -> new ResourceNotFoundException("Org unit not found"));
            user.setPrimaryOrgUnit(orgUnit);
        }

        User savedUser = userRepository.save(user);

        UserRole userRole = new UserRole();
        userRole.setUser(savedUser);
        userRole.setRole(role);
        userRole.setScopeType(request.scopeType() != null ? request.scopeType() : ScopeType.GLOBAL);
        userRole.setScopeId(request.scopeId());
        savedUser.getUserRoles().add(userRole);

        return toResponse(userRepository.findByIdWithRoles(savedUser.getId()).orElse(savedUser));
    }

    private User getCurrentPrincipalUser() {
        UserPrincipal principal = getCurrentPrincipal();
        return userRepository.findByIdWithRoles(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private UserPrincipal getCurrentPrincipal() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new BusinessException("Authentication required", "UNAUTHORIZED");
        }
        return principal;
    }

    private UserResponse toResponse(User user) {
        String role = user.getUserType().name();
        UUID primaryOrgUnitId = user.getPrimaryOrgUnit() != null ? user.getPrimaryOrgUnit().getId() : null;
        return new UserResponse(
                user.getId(),
                user.getTenant().getId(),
                user.getEmail(),
                user.getUserType(),
                user.getStatus(),
                primaryOrgUnitId,
                role
        );
    }
}
