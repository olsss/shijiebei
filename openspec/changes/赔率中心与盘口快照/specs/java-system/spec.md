# java-system Specification

## ADDED Requirements

### Requirement: Structured odds persistence

The system SHALL persist every market and every selection odds value from approved ODDS JSON into official business tables.

#### Scenario: Market and selections are stored

- **WHEN** an authenticated administrator approves an ODDS JSON item containing multiple markets and multiple selections
- **THEN** the backend stores one `odds_market_snapshots` row per market
- **AND** stores one `odds_selection_snapshots` row per selection odds value
- **AND** preserves bookmaker, market code, market name, snapshot type, handicap line, captured time, selection code, selection name, and odds value

#### Scenario: Legacy odds snapshot remains compatible

- **WHEN** an ODDS JSON item is approved
- **THEN** the backend still writes compatible `odds_snapshots` rows for existing overview and mapping behavior

#### Scenario: Object-shaped odds are expanded

- **WHEN** an ODDS JSON market contains odds as an object such as `{ "HOME": 1.80, "DRAW": 3.40, "AWAY": 4.20 }` or nested entries such as `{ "HOME": { "name": "主胜", "odds": 1.80 } }`
- **THEN** each object field is stored as an independent selection odds row
- **AND** nested entry name, odds value, implied probability, and status are preserved when present

### Requirement: Odds center API

The system SHALL expose authenticated odds center APIs for official odds data already stored in the business database.

#### Scenario: Odds center requires authentication

- **WHEN** an anonymous user requests `GET /api/odds`
- **THEN** the backend returns an unauthorized response

#### Scenario: Odds overview returns market summaries

- **WHEN** an authenticated administrator requests `GET /api/odds`
- **THEN** the backend returns market summaries with match, bookmaker, market, line, snapshot type, captured time, and selection count

#### Scenario: Match odds detail returns all selections

- **WHEN** an authenticated administrator requests `GET /api/odds/matches/{matchId}`
- **THEN** the backend returns all market snapshots for the match
- **AND** each market includes all stored selection odds rows

#### Scenario: Filter dictionaries are available

- **WHEN** an authenticated administrator requests bookmakers or markets endpoints
- **THEN** the backend returns distinct bookmaker and market values from official odds data

### Requirement: Odds center frontend integration

The frontend SHALL provide an odds center page connected to the odds center APIs.

#### Scenario: Odds API helper exists

- **WHEN** frontend tests import the odds API module
- **THEN** helpers exist for odds overview, match detail, bookmakers, and markets

#### Scenario: Dashboard links to odds center

- **WHEN** the dashboard module cards are rendered
- **THEN** the odds center card points to `/odds`

#### Scenario: Router exposes odds center

- **WHEN** frontend tests inspect router routes
- **THEN** a route exists for `/odds`
