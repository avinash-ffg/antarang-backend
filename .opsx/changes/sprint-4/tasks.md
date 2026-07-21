# Implementation Tasks

- [ ] 1. **Flyway Migrations:** `V6__reports_integrations_audit.sql`. Ensure ENUM values for audit and notifications strictly match design requirements.

- [ ] 2. **Algorithmic Processing Service:** Implement IAR career mapping weighting logic.

- [ ] 3. **S3 Metadata Integration Engine:** Mock file pointer save logic to `generated_reports`.

- [ ] 4. **Qualitative Notes Infrastructure:** Implement services for `CounsellorFeedback` filtering by student/internal visibility.

- [ ] 5. **Notification Template Resolution:** Implement language resolution fallback logic (User pref -> Tenant default).

- [ ] 6. **Automated AOP Audit Logger:** Implement `@Aspect` interceptor to capture exactly the 7 sensitive actions designated in the flow diagram.