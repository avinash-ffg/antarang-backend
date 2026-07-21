# System Design: Sprint 2 Metadata Layers

## Database Schema (Entities to Add)

*   `question_categories`, `questions`, `question_options`: Core bank entities.

*   `question_translations` & `option_translations`: Multi-language payload mappings.

*   `question_rules`: JSONB mapping step rules.

*   `questionnaires`, `questionnaire_versions`, `questionnaire_questions`: Layout definition and version locking.

*   `assessment_configurations`: Custom policy settings `timer_mode`, `max_duration_minutes`, `allow_resume`, `max_attempts`).

*   `assessment_configuration_groups`, `assessment_configuration_group_items`, `assessment_configuration_group_assignments`: Orchestrator entities.

*   **[NEW]** `report_templates`, `report_template_versions`: Skeleton tables required immediately to satisfy foreign keys in `assessment_configuration_group_items` prior to Sprint 4 completion.

## API Contracts to Implement

*   `POST /api/v1/questions`: Add structured questions.

*   `PUT /api/v1/questions/{questionId}/translations`: Maps multilingual array payloads.

*   `POST /api/v1/questionnaire-versions/{versionId}/publish`: Freezes version.

*   `POST /api/v1/assessment-configuration-groups`: Configures deployment parameters.