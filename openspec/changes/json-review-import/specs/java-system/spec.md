# java-system Specification

## ADDED Requirements

### Requirement: JSON archive scan job

The system SHALL scan the configured archive path and create an import job for JSON review.

#### Scenario: Scan creates a job

- **WHEN** an authenticated administrator requests a scan
- **THEN** the backend creates an import job
- **AND** the job records the archive path, total item count, valid item count, invalid item count, and creation time

### Requirement: JSON import item classification

The system SHALL classify supported archive files into import item types.

#### Scenario: Supported files are classified

- **WHEN** the scanner sees `bets.json`
- **THEN** it creates a `BETS` import item
- **WHEN** the scanner sees files under `analysis/*.json`
- **THEN** it creates `ANALYSIS` import items
- **WHEN** the scanner sees files under `odds/*.json`
- **THEN** it creates `ODDS` import items
- **WHEN** the scanner sees files under `sources/*.json`
- **THEN** it creates `SOURCE` import items

### Requirement: JSON validation and summary

The system SHALL validate JSON parseability and extract a human-readable summary before review.

#### Scenario: Valid JSON item

- **WHEN** a supported file contains valid JSON and required basic fields for its type
- **THEN** the import item is marked valid
- **AND** a summary title is stored
- **AND** a SHA-256 hash is stored

#### Scenario: Invalid JSON item

- **WHEN** a supported file cannot be parsed as JSON
- **THEN** the import item is marked invalid
- **AND** the validation message explains the parse failure
- **AND** the item cannot be approved

### Requirement: Review status transitions

The system SHALL support approval and rejection of pending import items.

#### Scenario: Approve a valid item

- **WHEN** an authenticated administrator approves a valid pending item
- **THEN** the item status changes to `APPROVED`
- **AND** the approval time is recorded
- **AND** an audit log entry is created

#### Scenario: Reject an item

- **WHEN** an authenticated administrator rejects a pending item with a reason
- **THEN** the item status changes to `REJECTED`
- **AND** the rejection reason is stored
- **AND** an audit log entry is created

#### Scenario: Approve invalid item is blocked

- **WHEN** an authenticated administrator attempts to approve an invalid item
- **THEN** the backend returns a 400 response
- **AND** the item remains pending review

### Requirement: CLI import command

The backend SHALL provide a command-line JSON import entry for local personal use.

#### Scenario: Dry run

- **WHEN** the application starts with `import-json --path <archive> --dry-run`
- **THEN** it scans the archive path
- **AND** prints a summary
- **AND** does not persist import jobs or items

#### Scenario: Approve run

- **WHEN** the application starts with `import-json --path <archive> --approve`
- **THEN** it scans the archive path
- **AND** persists valid import items as approved staging records

### Requirement: JSON review frontend

The frontend SHALL expose a JSON review center for scanning and reviewing import items.

#### Scenario: Review route exists

- **WHEN** the frontend router is created
- **THEN** a route exists for `/import-review`

#### Scenario: Review API helpers exist

- **WHEN** the frontend imports the review API module
- **THEN** it can call scan, list, detail, approve, batch approve, and reject functions
