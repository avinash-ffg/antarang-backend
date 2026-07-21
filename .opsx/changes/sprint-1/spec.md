# Specification: Foundation & Identity

## Requirements

### System & Architecture

* The system SHALL use PostgreSQL with UUIDs as primary keys for all tables.

* The system SHALL implement standard audit columns `created_at`, `updated_at`, `created_by`, `updated_by`, `is_active`, `is_deleted`) for all business tables.

* The system SHALL return all API responses in a standardized JSON format containing `success`, `message`, `data`, and `timestamp` fields.

### Authentication & Authorization

* The system SHALL authenticate users using JWT Bearer Tokens.

* The system SHALL enforce Role-Based Access Control (RBAC) supporting STUDENT, FACILITATOR, ADMIN, SUB_ADMIN, and SUPER_ADMIN roles.

* The system SHALL restrict Sub-Admin access based on assigned organizational hierarchy or cluster scope.

* The system SHALL log all authentication attempts (both success and failure).

## Scenarios

### Scenario: Successful Login

**GIVEN** a registered user exists with a valid email and password

**WHEN** the user submits their credentials via POST to `/api/v1/auth/login`

**THEN** the system returns a 200 OK

**AND** the payload includes a valid JWT `accessToken` and `refreshToken`

**AND** the user's role and primary organizational unit ID are included in the response data.

### Scenario: Unauthorized Access Attempt

**GIVEN** a user authenticated with the STUDENT role

**WHEN** they attempt to access an ADMIN-only endpoint (e.g., POST `/api/v1/org-units`)

**THEN** the system returns a 403 Forbidden

**AND** the error code is `ACCESS_DENIED`.