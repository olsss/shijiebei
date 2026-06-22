# java-system Specification

## ADDED Requirements

### Requirement: Match center query

The system SHALL expose authenticated match center APIs for official match data already stored in the business database.

#### Scenario: Match center requires authentication

- **WHEN** an anonymous user requests `GET /api/matches`
- **THEN** the backend returns an unauthorized response

#### Scenario: Match list returns schedule context

- **WHEN** an authenticated administrator requests `GET /api/matches`
- **THEN** the backend returns matches with matchday, kickoff time, JC code, competition, stage, venue, status, result status, home team, away team, and data completeness counts

#### Scenario: Missing match returns not found

- **WHEN** an authenticated administrator requests a match id that does not exist
- **THEN** the backend returns a not found response

### Requirement: Match detail aggregation

The system SHALL aggregate lineups, events, team stats, player stats, evidence, conflicts, and external factors for a match detail view.

#### Scenario: Match detail returns full context

- **WHEN** an authenticated administrator requests `GET /api/matches/{matchId}` for a match with context rows
- **THEN** the backend returns the match summary
- **AND** returns lineups with starter flags and player names
- **AND** returns events ordered by minute
- **AND** returns team stats with scoring minutes and first goal minute
- **AND** returns player stats with goals, assists, yellow cards, and red cards
- **AND** returns source evidence and data conflicts
- **AND** returns external factors from the match row

#### Scenario: Match subresources are independently queryable

- **WHEN** an authenticated administrator requests lineup, event, team-stat, or player-stat subresource endpoints
- **THEN** the backend returns the corresponding match-scoped rows

### Requirement: Match center frontend integration

The frontend SHALL provide a match center page connected to the match center APIs.

#### Scenario: Match API helper exists

- **WHEN** frontend tests import the matches API module
- **THEN** helpers exist for match list, match detail, lineups, events, team stats, and player stats

#### Scenario: Dashboard links to match center

- **WHEN** the dashboard module cards are rendered
- **THEN** the match center card points to `/matches`

#### Scenario: Router exposes match center

- **WHEN** frontend tests inspect router routes
- **THEN** a route exists for `/matches`
