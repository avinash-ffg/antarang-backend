# System Design: Sprint 3 Execution Layers

## Database Schema (Entities to Add)

*   `assessment_attempts`: Tracks `status` (IN_PROGRESS, SUBMITTED, EXPIRED), timestamps, and version references.

*   `assessment_responses`, `assessment_response_options`: Granular answer tracking.

*   `assessment_question_timings`: Maps question-level elapsed time.

*   `attempt_status_history`: Tracks transitions.

*   `scoring_rules`, `scoring_rule_versions`: Evaluation math properties.

*   `benchmarks`, `domain_scores`: Threshold mapping and outcome tracking.

## API Contracts to Implement

*   `GET /api/v1/students/{studentId}/assessments`: Lists assigned assessments.

*   `POST /api/v1/assessments/{assessmentId}/start`: Creates versioned attempt.

*   `PUT /api/v1/attempts/{attemptId}/responses`: Batch updates answers.

*   `POST /api/v1/attempts/{attemptId}/submit`: Executes mandatory validation and locks record.