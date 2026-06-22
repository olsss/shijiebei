# java-system Specification

## ADDED Requirements

### Requirement: Core business schema

The system SHALL provide persistent core business tables for approved World Cup data.

#### Scenario: Core schema migrates successfully

- **WHEN** the backend starts with Flyway enabled
- **THEN** the database contains tables for dictionaries, evidence, conflicts, teams, players, matches, match details, odds snapshots, analysis reports, bets, and import item mappings
- **AND** Hibernate schema validation succeeds

### Requirement: Approved import item mapping

The system SHALL import only approved and valid import items into core business tables.

#### Scenario: Approved item can be imported

- **WHEN** an authenticated administrator imports an `APPROVED` import item with `valid_json=true`
- **THEN** the backend maps recognizable fields into core business tables
- **AND** records at least one `import_item_mappings` row
- **AND** records an audit log entry

#### Scenario: Pending item is rejected

- **WHEN** an authenticated administrator imports a `PENDING_REVIEW` import item
- **THEN** the backend returns a 400 response
- **AND** no core business rows are created from that item

#### Scenario: Invalid item is rejected

- **WHEN** an authenticated administrator imports an approved import item with `valid_json=false`
- **THEN** the backend returns a 400 response
- **AND** no core business rows are created from that item

#### Scenario: Repeated import is idempotent

- **WHEN** an authenticated administrator imports the same approved import item more than once
- **THEN** the backend returns existing mappings
- **AND** does not duplicate target business rows

### Requirement: Business JSON mapping

The system SHALL map supported import item types into their target business tables.

#### Scenario: Analysis JSON maps to reports

- **WHEN** an approved `ANALYSIS` import item is imported
- **THEN** the backend upserts a match record
- **AND** creates an analysis report linked to the import item
- **AND** stores source evidence when source data is available

#### Scenario: Odds JSON maps to odds snapshots

- **WHEN** an approved `ODDS` import item is imported
- **THEN** the backend upserts a match record
- **AND** stores one or more odds snapshot rows or raw odds payload rows

#### Scenario: Source JSON maps to evidence and conflicts

- **WHEN** an approved `SOURCE` import item is imported
- **THEN** the backend stores source evidence rows
- **AND** stores conflict rows when conflict data is available
- **AND** stores dictionary aliases when alias data is available

#### Scenario: Bets JSON maps to bet records

- **WHEN** an approved `BETS` import item is imported
- **THEN** the backend stores bet records from `bets[]`
- **AND** upserts related match records when match fields are available

### Requirement: Core data API

The backend SHALL expose authenticated APIs for core data overview and import mappings.

#### Scenario: Overview endpoint returns counts

- **WHEN** an authenticated administrator requests `GET /api/core-data/overview`
- **THEN** the backend returns counts for matches, teams, players, analysis reports, bets, odds snapshots, evidence, and mappings

#### Scenario: Import endpoint imports a single item

- **WHEN** an authenticated administrator requests `POST /api/core-data/import-items/{itemId}/import`
- **THEN** the backend imports that item or returns existing mappings

#### Scenario: Mapping endpoint returns mappings

- **WHEN** an authenticated administrator requests `GET /api/core-data/import-items/{itemId}/mappings`
- **THEN** the backend returns mapping rows for that import item

### Requirement: Core data frontend integration

The frontend SHALL expose lightweight core data management affordances.

#### Scenario: Core data API helper exists

- **WHEN** frontend tests import the core data API module
- **THEN** helpers exist for overview, importing an item, and listing mappings

#### Scenario: Dashboard shows core data overview

- **WHEN** the dashboard is loaded
- **THEN** it includes a core data overview section

#### Scenario: Import review page can trigger core import

- **WHEN** an import review item is approved
- **THEN** the page exposes an action to import it into core business tables
