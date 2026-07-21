# Specification: Outputs & Platform Operations

## Requirements

### Reporting & Notifications

* The platform SHALL execute career mapping lookups matching computed evaluation totals against categorical mappings.

* **[NEW]** The platform SHALL resolve the user's localized language preference before rendering the notification template variables, falling back to the tenant's default language if a translation is missing.

### Operational Security

* The backend SHALL automatically record immutable system logging entries detailing actor information, old values, and new transformations.

## Scenarios

### Scenario: Event-Driven Audit Logging

**GIVEN** an Admin updates a scoring parameter

**WHEN** the transaction is committed

**THEN** the AOP Aspect automatically captures the event, old JSON value, and new JSON value, writing it to `audit_logs` without manual service intervention.