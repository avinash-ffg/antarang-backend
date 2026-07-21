# Change Proposal: Sprint 1 - Foundation, Auth, and User Management

## Intent

Build the foundational Spring Boot and PostgreSQL backend for the Antarang Career Assessment Platform (CAP) MVP. This change establishes the core architecture, multitenancy, and identity management system required before any assessment logic can be built.

## Scope

- Base project setup (Spring Boot, PostgreSQL, Flyway).

- Standardized API Response wrappers and Global Error Handling.

- Core Database Entities: Tenants, Users, Roles, Permissions, OrgUnits.

- JWT-based Authentication and Scoped Role-Based Access Control (RBAC).

## Non-Goals (Strictly Out of Scope)

- Question Bank, Questionnaires, Assessment Execution, and Scoring (Deferred to Sprints 2-4).

- Data Analyst and Guest user roles (Deferred to Phase 2).

- Notification services and external integrations.