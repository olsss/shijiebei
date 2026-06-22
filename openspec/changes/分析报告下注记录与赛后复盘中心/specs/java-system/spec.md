# java-system Specification

## ADDED Requirements

### Requirement: Approved analysis JSON persists AI bet plans and reviews

The system SHALL persist AI-generated bet plans and post-match reviews from approved ANALYSIS JSON into official business tables without generating new betting logic.

#### Scenario: Bet plan is stored from approved analysis JSON

- **WHEN** an authenticated administrator approves an ANALYSIS JSON item containing `bet_plan`, `bet_plans`, or `recommended_plan`
- **THEN** the backend stores one `bet_plans` row per plan
- **AND** preserves match, linked analysis report, plan key, title, conclusion type, confidence, budget amount, risk summary, betting method, strategy type, generator metadata, status, and raw payload

#### Scenario: Bet plan items are stored

- **WHEN** an approved ANALYSIS JSON bet plan contains `items`, `selections`, or `tickets`
- **THEN** the backend stores one `bet_plan_items` row per item
- **AND** preserves market type, selection text, stake suggestion, odds, line value, logic type, risk level, play type, pass type, order, and raw payload

#### Scenario: Post-match review is stored from analysis JSON

- **WHEN** an approved ANALYSIS JSON item contains `post_match_review` or `review`
- **THEN** the backend stores a `post_match_reviews` row
- **AND** preserves math, football, handicap, tournament temperament, odds value, overall summary, and raw payload

#### Scenario: Review lessons are stored

- **WHEN** a post-match review contains `lessons` or `rules`
- **THEN** the backend stores one `review_lessons` row per lesson
- **AND** preserves lesson type, lesson text, severity, and raw payload

#### Scenario: Existing analysis import remains compatible

- **WHEN** an ANALYSIS JSON item is approved
- **THEN** the backend still writes compatible `analysis_reports`, `source_evidence`, and import mappings for existing analysis behavior

### Requirement: Approved bet JSON persists settlement and CLV fields

The system SHALL persist approved BETS JSON actual betting records with ticket, settlement, and CLV fields.

#### Scenario: Bet record stores ticket and dates

- **WHEN** an authenticated administrator approves a BETS JSON item containing actual ticket records
- **THEN** each bet stores ticket number, bet date, matchday, market type, selection text, stake, odds, hit status, and raw payload

#### Scenario: Bet record stores settlement values

- **WHEN** an approved BETS JSON item contains return amount, profit/loss, settled time, closing odds, or CLV
- **THEN** each bet stores those settlement values in official columns

#### Scenario: CLV is calculated when missing

- **WHEN** an approved BETS JSON item has entry odds and closing odds but no CLV
- **THEN** the backend calculates CLV as `entry odds / closing odds - 1`

#### Scenario: Existing bet import remains compatible

- **WHEN** a legacy BETS JSON item without new fields is approved
- **THEN** the backend still imports compatible `bets` rows without requiring settlement fields

### Requirement: Analysis bet review center API

The system SHALL expose authenticated APIs for stored analysis reports, AI bet plans, actual bet records, and post-match reviews.

#### Scenario: Center API requires authentication

- **WHEN** an anonymous user requests `GET /api/analysis-review/overview`
- **THEN** the backend returns an unauthorized response

#### Scenario: Overview returns statistics

- **WHEN** an authenticated administrator requests `GET /api/analysis-review/overview`
- **THEN** the backend returns report count, bet plan count, bet count, review count, total stake, total return, net profit, ROI, and average CLV

#### Scenario: Reports are queryable

- **WHEN** an authenticated administrator requests report list or report detail endpoints
- **THEN** the backend returns stored analysis reports with linked bet plans and reviews where applicable

#### Scenario: Bet plans are queryable

- **WHEN** an authenticated administrator requests bet plan list or detail endpoints
- **THEN** the backend returns stored AI bet plans and their items

#### Scenario: Bets and reviews are queryable

- **WHEN** an authenticated administrator requests bet or review endpoints
- **THEN** the backend returns actual bet records and post-match reviews from official tables

### Requirement: Analysis bet review frontend integration

The frontend SHALL provide an analysis, betting, and post-match review page connected to the center APIs.

#### Scenario: API helper exists

- **WHEN** frontend tests import the analysis-review API module
- **THEN** helpers exist for overview, reports, report detail, bet plans, plan detail, bets, and reviews

#### Scenario: Dashboard links to center

- **WHEN** dashboard module cards are rendered
- **THEN** the analysis bet review card points to `/analysis-review`

#### Scenario: Router exposes center

- **WHEN** frontend tests inspect router routes
- **THEN** a route exists for `/analysis-review`

#### Scenario: Page separates plan and actual bet data

- **WHEN** an administrator opens `/analysis-review`
- **THEN** the page shows AI bet plans separately from actual bet records
- **AND** the page distinguishes archived AI-generated plan content from real ticket settlement data

### Requirement: Betting generation boundaries

The system SHALL archive approved AI bet plan JSON without generating new betting recommendations.

#### Scenario: Java system does not infer new picks

- **WHEN** analysis, odds, sentiment, or bet data are displayed
- **THEN** the Java system does not compute new selections, stake sizes, add-on bets, guaranteed picks, or recovery betting instructions

#### Scenario: Archived AI plan content remains traceable

- **WHEN** an AI bet plan is displayed
- **THEN** the frontend shows it as approved archived JSON content with raw payload traceability
- **AND** does not present it as a newly generated Java recommendation
