# Specification: Content Management & Configurations

## Requirements

### Question Bank & Localization

* The system SHALL organize questions under explicit Question Categories tied to `assessment_type`.

* The system SHALL enforce localized question and option texts matching standard `language_id` references.

### Version Control & Configuration Lifecycle

* Questionnaire definitions SHALL remain immutable once their status transitions to `PUBLISHED`.

* The system SHALL use `assessment_configuration_groups` as the absolute programmatic binding module that maps an assessment version to its target deployment scopes.

* **[NEW]** The system SHALL resolve active Configuration Groups using strict priority routing: User Assignment (Highest) > Cluster Assignment > Org Unit Assignment (Lowest), as defined in the resolution flow.

## Scenarios

### Scenario: Publishing a Questionnaire Version

**GIVEN** a Questionnaire Version exists in a `DRAFT` state

**WHEN** an authorized ADMIN calls the publication endpoint

**THEN** the system marks the version status as `PUBLISHED`

**AND** freezes any subsequent structural schema edits.