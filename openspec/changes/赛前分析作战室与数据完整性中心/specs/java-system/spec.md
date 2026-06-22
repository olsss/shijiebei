# java-system Specification

## ADDED Requirements

### Requirement: Prematch workbench API

The system SHALL expose authenticated APIs that aggregate official business data by match for prematch review.

#### Scenario: Prematch workbench requires authentication

- **WHEN** an anonymous user requests `GET /api/prematch-workbench/matches`
- **THEN** the backend returns an unauthorized response

#### Scenario: Match list returns workbench summaries

- **WHEN** an authenticated administrator requests `GET /api/prematch-workbench/matches`
- **THEN** the backend returns match summaries with match identity, team names, data counts, integrity score, missing count, stale count, and conflict count

#### Scenario: Match detail aggregates business data

- **WHEN** an authenticated administrator requests `GET /api/prematch-workbench/matches/{matchId}`
- **THEN** the backend returns match summary, teams, lineups, players, odds markets, sentiment factors, source evidence, data conflicts, analysis reports, AI bet plans, actual bets, and integrity checks for that match

#### Scenario: Integrity endpoint returns checks

- **WHEN** an authenticated administrator requests `GET /api/prematch-workbench/matches/{matchId}/integrity`
- **THEN** the backend returns data integrity checks for the match without requiring the full workbench payload

#### Scenario: Unknown match returns not found

- **WHEN** an authenticated administrator requests a prematch workbench endpoint for an unknown match id
- **THEN** the backend returns a not found response

### Requirement: Data integrity checks

The system SHALL calculate match-level data completeness checks from already stored official business data.

#### Scenario: Required data categories are checked

- **WHEN** a match workbench summary or detail is requested
- **THEN** the backend evaluates team profile, player profile, lineup, odds market, live odds freshness, sentiment factor, analysis report, AI bet plan, multi-source evidence, and unresolved conflict checks

#### Scenario: Missing data is reported

- **WHEN** a required data category is absent or below its threshold
- **THEN** the related check status is `MISSING`
- **AND** the check message describes the missing category without generating a betting recommendation

#### Scenario: Stale data is reported

- **WHEN** live odds are older than 3 hours or sentiment/external factors are expired
- **THEN** the related check status is `STALE`
- **AND** the workbench stale count increases

#### Scenario: Conflicts are reported

- **WHEN** unresolved data conflicts exist for a match
- **THEN** the unresolved conflict check status is `CONFLICT`
- **AND** the workbench conflict count increases

#### Scenario: Integrity score reflects data readiness only

- **WHEN** checks are evaluated
- **THEN** the integrity score is calculated from passed checks divided by total checks
- **AND** the score is not described as win probability, betting value, or confidence to stake

### Requirement: Prematch workbench frontend integration

The frontend SHALL provide a prematch workbench page connected to the prematch workbench APIs.

#### Scenario: API helper exists

- **WHEN** frontend tests import the prematch workbench API module
- **THEN** helpers exist for match list, match detail, and integrity endpoint paths

#### Scenario: Dashboard links to prematch workbench

- **WHEN** dashboard module cards are rendered
- **THEN** the prematch workbench card points to `/prematch-workbench`

#### Scenario: Router exposes prematch workbench

- **WHEN** frontend tests inspect router routes
- **THEN** a route exists for `/prematch-workbench`

#### Scenario: Page separates data readiness from betting recommendations

- **WHEN** an administrator opens `/prematch-workbench`
- **THEN** the page displays data integrity status, match aggregation, and archived AI analysis content
- **AND** the page does not present newly generated betting selections, stake sizes, add-on bets, guaranteed picks, or recovery betting instructions

### Requirement: Prematch workbench boundaries

The system SHALL aggregate and validate approved data without generating new betting logic.

#### Scenario: Java system does not infer new picks

- **WHEN** match, team, player, odds, sentiment, analysis, bet plan, or bet data are displayed in the prematch workbench
- **THEN** the Java system does not compute new betting selections, stake sizes, or betting methods

#### Scenario: Data gaps are framed as review prompts

- **WHEN** data is missing, stale, or conflicting
- **THEN** the frontend describes it as a data review or replenishment prompt
- **AND** does not convert the issue into a concrete betting action
