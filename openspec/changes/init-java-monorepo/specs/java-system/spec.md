# java-system Specification

## ADDED Requirements

### Requirement: Java system monorepo baseline

The project SHALL contain a Java system baseline with separate backend and frontend directories while preserving the existing `skill/` analysis system.

#### Scenario: Backend and frontend directories exist

- **WHEN** a developer lists the repository root
- **THEN** `server/` exists for Spring Boot backend code
- **AND** `client/` exists for Vue frontend code
- **AND** `skill/` remains present and unmodified by this change

### Requirement: Backend health and settings APIs

The backend SHALL expose health and system settings endpoints for the personal management system.

#### Scenario: Health endpoint responds

- **WHEN** `GET /api/health` is requested
- **THEN** the response status is 200
- **AND** the response body contains `status` equal to `UP`

#### Scenario: Settings endpoint reports archive path

- **WHEN** an authenticated administrator requests `GET /api/system/settings`
- **THEN** the response status is 200
- **AND** the response body contains the configured JSON archive path

### Requirement: Single administrator authentication

The backend SHALL support a single administrator login for personal use.

#### Scenario: Valid administrator credentials

- **WHEN** `POST /api/auth/login` is called with configured credentials
- **THEN** the response status is 200
- **AND** the response body identifies the administrator user

#### Scenario: Invalid administrator credentials

- **WHEN** `POST /api/auth/login` is called with an incorrect password
- **THEN** the response status is 401

### Requirement: Frontend shell

The frontend SHALL provide a Vue3 shell with login, dashboard, and system settings pages.

#### Scenario: Routes are registered

- **WHEN** the frontend router is created
- **THEN** routes exist for `/login`, `/`, and `/settings`

### Requirement: Review gate before delivery

Each stage delivery SHALL pass automated verification and human or subagent review before being reported as deliverable.

#### Scenario: Stage verification is complete

- **WHEN** a stage is ready for delivery
- **THEN** OpenSpec validation, backend tests, frontend tests, frontend build, and review gate checks have recorded passing evidence
