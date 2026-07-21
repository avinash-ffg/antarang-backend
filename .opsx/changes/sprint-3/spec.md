# Specification: Assessment Lifecycle & Analytics

## Requirements

### Runtime Assessment State Controls

* The system SHALL enforce a single-active-relationship index constraint restricting concurrent active attempts per student.

* **[NEW]** The system SHALL validate that all questions marked as `is_mandatory` in the active questionnaire version contain valid responses before allowing the status to change to `SUBMITTED`.

* **[NEW]** The system SHALL evaluate the `allow_resume` flag upon session interruption; if true, the student resumes the same attempt; if false (Restart configured), the system SHALL mark the interrupted attempt as incomplete/restarted and generate a new attempt record.

### Score Computations

* The system SHALL map computed outcome scores to explicit language-independent profile brackets based on benchmark thresholds.

## Scenarios

### Scenario: Graceful Submission

**GIVEN** an active attempt record is `IN_PROGRESS`

**AND** all mandatory questions are answered

**WHEN** a POST submission is processed

**THEN** the system calculates overall `elapsed_seconds`, updates status to `SUBMITTED`, and triggers the scoring engine.