# java-system Specification

## ADDED Requirements

### Requirement: Profile collection staging

The system SHALL stage AI or external team/player profile facts before they become official business data.

#### Scenario: Collection item can be reviewed

- **WHEN** an authenticated administrator lists collection items filtered by status
- **THEN** the backend returns staged collection items with entity type, entity key, fact type, summary, source, reliability, and review status

#### Scenario: Approved team collection item writes a team fact

- **WHEN** an authenticated administrator approves a pending `TEAM` collection item whose `entity_key` matches an existing team
- **THEN** the backend creates a `team_profile_facts` row linked to the collection item
- **AND** marks the collection item as `APPROVED`

#### Scenario: Approved player collection item writes a player fact

- **WHEN** an authenticated administrator approves a pending `PLAYER` collection item whose `entity_key` matches an existing player
- **THEN** the backend creates a `player_profile_facts` row linked to the collection item
- **AND** marks the collection item as `APPROVED`

#### Scenario: Unknown entity is rejected

- **WHEN** an authenticated administrator approves a collection item whose entity key does not exist
- **THEN** the backend returns a 400 response
- **AND** no profile fact row is created

#### Scenario: Approval is idempotent

- **WHEN** an authenticated administrator approves the same collection item more than once
- **THEN** the backend returns the existing fact mapping
- **AND** does not duplicate profile fact rows

#### Scenario: Collection item can be rejected

- **WHEN** an authenticated administrator rejects a pending collection item with a review note
- **THEN** the backend marks the item as `REJECTED`
- **AND** no official profile fact row is created

### Requirement: Team profile query

The system SHALL expose authenticated team profile list and detail APIs.

#### Scenario: Team list returns profile summaries

- **WHEN** an authenticated administrator requests `GET /api/profiles/teams`
- **THEN** the backend returns teams with player count, fact count, and latest profile update time

#### Scenario: Team detail returns facts and players

- **WHEN** an authenticated administrator requests `GET /api/profiles/teams/{teamId}`
- **THEN** the backend returns the team's base profile, profile facts, players, evidence count, and conflict count

#### Scenario: Team players endpoint returns squad

- **WHEN** an authenticated administrator requests `GET /api/profiles/teams/{teamId}/players`
- **THEN** the backend returns players belonging to the team with status, injury, card, and locker-room fields

### Requirement: Player profile query

The system SHALL expose authenticated player profile list and detail APIs.

#### Scenario: Player list returns profile summaries

- **WHEN** an authenticated administrator requests `GET /api/profiles/players`
- **THEN** the backend returns players with team name, status, injury, card, locker-room summary, fact count, and latest profile update time

#### Scenario: Player detail returns facts

- **WHEN** an authenticated administrator requests `GET /api/profiles/players/{playerId}`
- **THEN** the backend returns the player's base profile, team name, and profile facts

### Requirement: Profile frontend integration

The frontend SHALL provide team and player profile centers.

#### Scenario: Profile API helper exists

- **WHEN** frontend tests import the profiles API module
- **THEN** helpers exist for team list/detail, team players, player list/detail, collection item listing, approval, and rejection

#### Scenario: Dashboard links to profile centers

- **WHEN** the dashboard module cards are rendered
- **THEN** the team and player cards point to `/profiles/teams` and `/profiles/players`

#### Scenario: Router exposes profile pages

- **WHEN** frontend tests inspect router routes
- **THEN** routes exist for `/profiles/teams` and `/profiles/players`
