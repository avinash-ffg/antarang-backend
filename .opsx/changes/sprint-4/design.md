# System Design: Sprint 4 Advanced Modules

## Database Schema (Entities to Add)

*   `career_clusters`, `careers`, `career_mappings`: Relational algorithm matrices.

*   `recommendation_runs`, `recommendations`: Ranked matches logic.

*   `generated_reports`: S3 URL string pointers.

*   `counsellor_feedback`: Captures guidance notes with `visibility` flags.

*   `integration_clients`, `external_id_mappings`, `integration_events`: Partner syncing.

*   **[NEW]** `notification_logs`: Must enforce enums: `ACCOUNT_CREATED`, `PASSWORD_RESET`, `ASSESSMENT_ASSIGNED`, `ASSESSMENT_COMPLETED`, `REPORT_PUBLISHED`.

*   **[NEW]** `audit_logs`: AOP must map to specific actions: `User update`, `Role assignment`, `Questionnaire publish`, `IAR/scoring update`, `Configuration group assignment`, `Assessment submit`, `Report download`.

## API Contracts to Implement

*   `POST /api/v1/recommendations/generate/{studentId}`: Generates a recommendation run.

*   `POST /api/v1/reports/generate/{studentId}`: Compiles performance outputs mock S3 URL.

*   `POST /api/v1/students/{studentId}/feedback`: Saves qualitative counsellor assessment entries.

*   `POST /api/v1/integrations/students`: Handles inbound partner data creation requests.