# System Design: Sprint 1 Foundation

## Tech Stack

* **Framework:** Spring Boot 3.x (Java 17+)

* **Database:** PostgreSQL

* **ORM:** Spring Data JPA / Hibernate

* **Security:** Spring Security + JWT

* **Migrations:** Flyway

## Database Schema (Core Entities)

*Note: Entities must follow standard* `snake_case` *naming in the database and* `camelCase` *in Java.*

*   `tenants`: `id` (UUID), `code`, `name`, `branding_config` (JSONB).

*   `org_units`: `id` (UUID), `tenant_id`, `parent_org_unit_id`, `org_unit_type` (STATE, DISTRICT, INSTITUTION, SCHOOL, PROGRAM, COHORT), `code`, `name`.

*   `users`: `id` (UUID), `tenant_id`, `primary_org_unit_id`, `email`, `password_hash`, `user_type`, `status`.

*   `roles` & `permissions`: Standard RBAC setup mapped via `role_permissions` and `user_roles`. The `user_roles` table must include `scope_type` and `scope_id` to support Sub-Admin isolation.

## API Contracts 

### Standard Response Wrapper

All successful API endpoints must wrap their payload in this structure:

```json

{

  "success": true,

  "message": "String",

  "data": {},

  "timestamp": "ISO-8601"

}