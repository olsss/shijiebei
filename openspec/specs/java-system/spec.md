# java-system Specification

## Purpose

???????????????????????? JSON ?????????????????????

## Requirements

### Requirement: Database authoritative data source
The system SHALL treat the database as the only authoritative long-term data source for World Cup management data.

#### Scenario: System pages read authoritative data
- **WHEN** a user opens match, odds, profile, workbench, decision, or review pages
- **THEN** the system reads data from database-backed APIs
- **AND** the system does not read local JSON files as the source of truth

#### Scenario: Raw JSON remains auditable in database
- **WHEN** a JSON file is scanned into the review workflow
- **THEN** the system stores the original JSON in `import_items.raw_json`
- **AND** mapped business rows keep raw payload data where the target table supports it

### Requirement: Data inbox directory workflow
The system SHALL use `data-inbox` as the local temporary JSON delivery area.

#### Scenario: Default scan path uses pending inbox
- **WHEN** an administrator opens the JSON review page or runs the CLI without an explicit path
- **THEN** the default scan path is `../data-inbox/pending`

#### Scenario: Inbox directories exist
- **WHEN** the repository is checked out
- **THEN** `data-inbox/pending`, `data-inbox/imported`, `data-inbox/rejected`, and `data-inbox/templates` are present or can be created by the system

### Requirement: Type based JSON import classification
The system SHALL classify new JSON import files by their internal `type` field.

#### Scenario: Type field classifies a flat JSON file
- **WHEN** the scanner sees a valid JSON file under `data-inbox/pending`
- **AND** the file contains `type: "MATCH"`
- **THEN** it creates a `MATCH` import item
- **AND** the item summary comes from payload match fields or the file name

#### Scenario: Missing type is invalid for new inbox files
- **WHEN** the scanner sees a JSON file under `data-inbox/pending`
- **AND** the file does not contain a supported `type`
- **THEN** the scanner marks the item invalid
- **AND** the validation message explains that `type` is required

#### Scenario: Legacy archive layout remains readable
- **WHEN** the scanner is explicitly pointed at an old archive layout containing `bets.json`, `analysis/*.json`, `odds/*.json`, or `sources/*.json`
- **THEN** it can classify those legacy files using the existing path-based rules

### Requirement: Supported import item types
The system SHALL support import item types for master data, evidence data, odds data, analysis data, betting data, and review data.

#### Scenario: Master data types are supported
- **WHEN** the scanner reads JSON files with `type` equal to `TEAM`, `PLAYER`, `MATCH`, `MATCH_LINEUP`, `MATCH_EVENT`, or `MATCH_STATS`
- **THEN** it creates valid import items when required payload fields are present

#### Scenario: Business data types are supported
- **WHEN** the scanner reads JSON files with `type` equal to `SOURCE`, `ODDS`, `ANALYSIS`, `BET_PLAN`, `BET`, `POST_REVIEW`, or `REVIEW_LESSON`
- **THEN** it creates valid import items when required payload fields are present

### Requirement: Master data mapping
The system SHALL map approved master data import items into core business tables with natural-key idempotency.

#### Scenario: Team JSON maps to teams
- **WHEN** an approved `TEAM` import item is imported into core data
- **THEN** the backend inserts or updates a row in `teams` using `team_key`
- **AND** it records an `import_item_mappings` row for the target team

#### Scenario: Player JSON maps to players
- **WHEN** an approved `PLAYER` import item is imported into core data
- **THEN** the backend inserts or updates a row in `players` using `player_key`
- **AND** it links the player to a team when `team_key` resolves to an existing team

#### Scenario: Match JSON maps to matches
- **WHEN** an approved `MATCH` import item is imported into core data
- **THEN** the backend inserts or updates a row in `matches` using `match_key`
- **AND** it links home and away teams when their team keys resolve

#### Scenario: Lineup event and stats JSON map to match detail tables
- **WHEN** approved `MATCH_LINEUP`, `MATCH_EVENT`, or `MATCH_STATS` items are imported
- **THEN** the backend writes rows to `match_lineups`, `match_events`, or `match_team_stats`
- **AND** the rows are linked to the resolved match and team/player records when keys resolve

### Requirement: Business data mapping from inbox JSON
The system SHALL map approved inbox business data into existing decision and review tables.

#### Scenario: Analysis and bet plan JSON maps to analysis tables
- **WHEN** approved `ANALYSIS` or `BET_PLAN` import items are imported
- **THEN** the backend stores analysis reports, bet plans, and bet plan items as applicable
- **AND** the records are linked to the resolved match when possible

#### Scenario: Bet and post-review JSON maps to review tables
- **WHEN** approved `BET`, `POST_REVIEW`, or `REVIEW_LESSON` import items are imported
- **THEN** the backend stores bet records, post-match reviews, and review lessons as applicable
- **AND** repeated import of the same item returns existing mappings instead of duplicating rows

### Requirement: Import file archival lifecycle
The system SHALL move local JSON files out of the pending inbox after a terminal review/import outcome.

#### Scenario: Successful core import archives file
- **WHEN** an approved import item is successfully imported into core business tables
- **THEN** the system moves the source JSON file from `data-inbox/pending` to `data-inbox/imported/<date>/`
- **AND** subsequent scans of `pending` do not re-create the same item from that file

#### Scenario: Rejected item archives file
- **WHEN** an administrator rejects an import item
- **THEN** the system moves the source JSON file from `data-inbox/pending` to `data-inbox/rejected/<date>/`
- **AND** the rejection reason remains stored in the database

### Requirement: Temporary JSON templates
The repository SHALL include JSON templates for supported inbox data types.

#### Scenario: Templates guide AI and manual data entry
- **WHEN** a user wants to generate a new temporary JSON import file
- **THEN** `data-inbox/templates` contains example templates for the core supported types
- **AND** each template uses the `type`, `idempotency_key`, `source`, and `payload` envelope

### Requirement: Skill write boundary
The system SHALL preserve an审核 gate between AI-generated analysis and authoritative database writes.

#### Scenario: Skill output enters through inbox
- **WHEN** a football analysis skill produces analysis, bet plan, or review data
- **THEN** it writes or returns temporary JSON for `data-inbox/pending`
- **AND** the data becomes authoritative only after administrator approval and core import
