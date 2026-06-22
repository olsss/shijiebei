# java-system Specification

## ADDED Requirements

### Requirement: Sentiment and external factor persistence

The system SHALL persist approved SOURCE JSON sentiment and external factor records into official business tables.

#### Scenario: External factors are stored

- **WHEN** an authenticated administrator approves a SOURCE JSON item containing `external_factors` or `factors`
- **THEN** the backend stores one `match_context_factors` row per factor
- **AND** preserves match, category, type, title, summary, impact direction, entity, evidence level, source, observed time, expiration time, confidence score, reliability score, and raw payload

#### Scenario: Sentiment records are stored

- **WHEN** an approved SOURCE JSON item contains `sentiment_records` or `sentiments`
- **THEN** each sentiment record is stored as a `match_context_factors` row
- **AND** the record remains distinguishable by category and evidence level

#### Scenario: Risk assessments are stored

- **WHEN** an approved SOURCE JSON item contains factor-level `risks` or top-level `risk_assessments`
- **THEN** the backend stores one `sentiment_risk_assessments` row per risk
- **AND** preserves risk type, risk level, risk score, rationale, suggested action, source, and raw payload

#### Scenario: Existing source import behavior remains compatible

- **WHEN** a SOURCE JSON item is approved
- **THEN** the backend still writes compatible `source_evidence`, `data_conflicts`, aliases, and import mappings for existing source behavior

### Requirement: Sentiment center API

The system SHALL expose authenticated sentiment center APIs for official sentiment and external factor data already stored in the business database.

#### Scenario: Sentiment center requires authentication

- **WHEN** an anonymous user requests `GET /api/sentiment`
- **THEN** the backend returns an unauthorized response

#### Scenario: Sentiment overview returns summaries

- **WHEN** an authenticated administrator requests `GET /api/sentiment`
- **THEN** the backend returns factor summaries with match, category, title, impact direction, source, reliability, confidence, stale flag, risk count, and highest risk level

#### Scenario: Match sentiment detail returns factors and risks

- **WHEN** an authenticated administrator requests `GET /api/sentiment/matches/{matchId}`
- **THEN** the backend returns all context factors for the match
- **AND** returns all risk assessments for the match

#### Scenario: Filter dictionaries are available

- **WHEN** an authenticated administrator requests categories or risk type endpoints
- **THEN** the backend returns distinct factor categories and risk types from official sentiment data

### Requirement: Sentiment center frontend integration

The frontend SHALL provide a sentiment and external factors page connected to the sentiment center APIs.

#### Scenario: Sentiment API helper exists

- **WHEN** frontend tests import the sentiment API module
- **THEN** helpers exist for sentiment overview, match detail, categories, and risk types

#### Scenario: Dashboard links to sentiment center

- **WHEN** the dashboard module cards are rendered
- **THEN** the sentiment center card points to `/sentiment`

#### Scenario: Router exposes sentiment center

- **WHEN** frontend tests inspect router routes
- **THEN** a route exists for `/sentiment`

### Requirement: Risk scoring boundaries

The system SHALL store and display risk scoring without generating betting recommendations.

#### Scenario: Suggested actions are risk-management only

- **WHEN** risk assessments are imported or queried
- **THEN** suggested actions are represented as verification, monitoring, or confidence-management actions
- **AND** the sentiment center does not output betting selections, stake sizes, or guaranteed picks
