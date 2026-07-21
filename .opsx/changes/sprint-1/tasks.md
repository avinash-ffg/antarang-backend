# Implementation Tasks - 

[x] 1. **Project Initialization:** Bootstrap the Spring Boot project with dependencies. Configure `application.yml` for DB connectivity. - 

[x] 2. **Database Migration:** Create `V1__core_tenant_org_user_rbac.sql` for tenants, org_units, users, roles, permissions. - 

[x] 3. **Core Architecture:** Implement `ApiResponse<T>`. - 

[x] 4. **Exception Handling:** Implement `@RestControllerAdvice` mapping exceptions to standard Error format. - 

[x] 5. **Base Entity:** Create `@MappedSuperclass` with audit fields `createdAt`, `updatedAt`, `createdBy`, `updatedBy`, `isActive`, `isDeleted`). - 

[x] 6. **Domain Entities:** Implement Tenant, OrgUnit, User, Role, and Permission JPA entities. - 

[x] 7. **Repositories:** Create Spring Data JPA repositories. - 

[x] 8. **Security Configuration:** Implement JWT Utility service. - 

[x] 9. **Security Filters:** Configure Spring Security filter chain (public auth, restricted api). - 

[x] 10. **Services:** Implement `AuthService` and `UserService`. - 

[x] 11. **Controllers:** Implement `AuthController` and `UserController`.
