CREATE TABLE data_dictionaries (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    dict_type VARCHAR(80) NOT NULL,
    code VARCHAR(160) NOT NULL,
    display_name VARCHAR(240) NOT NULL,
    alias VARCHAR(240) NOT NULL DEFAULT '',
    description VARCHAR(1000),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uk_data_dictionaries_type_code_alias ON data_dictionaries(dict_type, code, alias);

CREATE TABLE teams (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    team_key VARCHAR(240) NOT NULL,
    display_name VARCHAR(240) NOT NULL,
    fifa_code VARCHAR(20),
    country_region VARCHAR(120),
    style_tags TEXT,
    attack_profile TEXT,
    defense_profile TEXT,
    public_sentiment TEXT,
    raw_payload LONGTEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uk_teams_key ON teams(team_key);

CREATE TABLE players (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_key VARCHAR(240) NOT NULL,
    team_id BIGINT,
    display_name VARCHAR(240) NOT NULL,
    shirt_number INT,
    position VARCHAR(80),
    status VARCHAR(80),
    injury_status VARCHAR(240),
    card_status VARCHAR(240),
    locker_room_status VARCHAR(500),
    raw_payload LONGTEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_players_team FOREIGN KEY (team_id) REFERENCES teams(id)
);
CREATE UNIQUE INDEX uk_players_key ON players(player_key);
CREATE INDEX idx_players_team ON players(team_id);

CREATE TABLE matches (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    match_key VARCHAR(500) NOT NULL,
    match_name VARCHAR(300) NOT NULL,
    matchday DATE,
    jc_code VARCHAR(80),
    competition VARCHAR(160),
    stage VARCHAR(120),
    venue VARCHAR(240),
    kickoff_time TIMESTAMP NULL,
    home_team_id BIGINT,
    away_team_id BIGINT,
    status VARCHAR(80) NOT NULL DEFAULT 'IMPORTED',
    result_status VARCHAR(80) NOT NULL DEFAULT 'UNKNOWN',
    external_factors LONGTEXT,
    raw_payload LONGTEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_matches_home_team FOREIGN KEY (home_team_id) REFERENCES teams(id),
    CONSTRAINT fk_matches_away_team FOREIGN KEY (away_team_id) REFERENCES teams(id)
);
CREATE UNIQUE INDEX uk_matches_key ON matches(match_key);
CREATE INDEX idx_matches_matchday ON matches(matchday);

CREATE TABLE match_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    match_id BIGINT NOT NULL,
    event_minute INT,
    event_type VARCHAR(120) NOT NULL,
    team_id BIGINT,
    player_id BIGINT,
    payload LONGTEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_match_events_match FOREIGN KEY (match_id) REFERENCES matches(id),
    CONSTRAINT fk_match_events_team FOREIGN KEY (team_id) REFERENCES teams(id),
    CONSTRAINT fk_match_events_player FOREIGN KEY (player_id) REFERENCES players(id)
);
CREATE INDEX idx_match_events_match ON match_events(match_id);

CREATE TABLE match_team_stats (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    match_id BIGINT NOT NULL,
    team_id BIGINT,
    stats_type VARCHAR(80) NOT NULL DEFAULT 'IMPORTED',
    goals_for INT,
    goals_against INT,
    first_goal_minute INT,
    scoring_minutes TEXT,
    payload LONGTEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_match_team_stats_match FOREIGN KEY (match_id) REFERENCES matches(id),
    CONSTRAINT fk_match_team_stats_team FOREIGN KEY (team_id) REFERENCES teams(id)
);
CREATE INDEX idx_match_team_stats_match ON match_team_stats(match_id);

CREATE TABLE match_player_stats (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    match_id BIGINT NOT NULL,
    player_id BIGINT,
    minutes_played INT,
    goals INT,
    assists INT,
    yellow_cards INT,
    red_cards INT,
    payload LONGTEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_match_player_stats_match FOREIGN KEY (match_id) REFERENCES matches(id),
    CONSTRAINT fk_match_player_stats_player FOREIGN KEY (player_id) REFERENCES players(id)
);
CREATE INDEX idx_match_player_stats_match ON match_player_stats(match_id);

CREATE TABLE match_lineups (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    match_id BIGINT NOT NULL,
    team_id BIGINT,
    player_id BIGINT,
    role VARCHAR(80),
    position VARCHAR(80),
    is_starter BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_match_lineups_match FOREIGN KEY (match_id) REFERENCES matches(id),
    CONSTRAINT fk_match_lineups_team FOREIGN KEY (team_id) REFERENCES teams(id),
    CONSTRAINT fk_match_lineups_player FOREIGN KEY (player_id) REFERENCES players(id)
);
CREATE INDEX idx_match_lineups_match ON match_lineups(match_id);

CREATE TABLE source_evidence (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    import_item_id BIGINT,
    match_id BIGINT,
    source_type VARCHAR(120) NOT NULL,
    source_name VARCHAR(240) NOT NULL,
    source_ref VARCHAR(500),
    source_url VARCHAR(1000),
    evidence_time TIMESTAMP NULL,
    summary TEXT,
    reliability_score DECIMAL(5,2),
    raw_payload LONGTEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_source_evidence_import_item FOREIGN KEY (import_item_id) REFERENCES import_items(id),
    CONSTRAINT fk_source_evidence_match FOREIGN KEY (match_id) REFERENCES matches(id)
);
CREATE INDEX idx_source_evidence_import_item ON source_evidence(import_item_id);
CREATE INDEX idx_source_evidence_match ON source_evidence(match_id);

CREATE TABLE data_conflicts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    import_item_id BIGINT,
    match_id BIGINT,
    conflict_type VARCHAR(120) NOT NULL,
    entity_key VARCHAR(300),
    field_name VARCHAR(160),
    current_value TEXT,
    incoming_value TEXT,
    resolution_status VARCHAR(80) NOT NULL DEFAULT 'PENDING',
    raw_payload LONGTEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_data_conflicts_import_item FOREIGN KEY (import_item_id) REFERENCES import_items(id),
    CONSTRAINT fk_data_conflicts_match FOREIGN KEY (match_id) REFERENCES matches(id)
);
CREATE INDEX idx_data_conflicts_import_item ON data_conflicts(import_item_id);

CREATE TABLE odds_snapshots (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    import_item_id BIGINT NOT NULL,
    match_id BIGINT,
    bookmaker VARCHAR(240) NOT NULL,
    market_type VARCHAR(120) NOT NULL DEFAULT 'RAW',
    odds_value DECIMAL(12,4),
    captured_at TIMESTAMP NULL,
    raw_payload LONGTEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_odds_snapshots_import_item FOREIGN KEY (import_item_id) REFERENCES import_items(id),
    CONSTRAINT fk_odds_snapshots_match FOREIGN KEY (match_id) REFERENCES matches(id)
);
CREATE INDEX idx_odds_snapshots_import_item ON odds_snapshots(import_item_id);
CREATE INDEX idx_odds_snapshots_match ON odds_snapshots(match_id);

CREATE TABLE analysis_reports (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    import_item_id BIGINT NOT NULL,
    match_id BIGINT,
    analysis_id VARCHAR(160) NOT NULL,
    conclusion_type VARCHAR(160),
    confidence VARCHAR(80),
    risk_summary TEXT,
    recommended_markets LONGTEXT,
    dimensions LONGTEXT,
    narrative_md LONGTEXT,
    raw_payload LONGTEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_analysis_reports_import_item FOREIGN KEY (import_item_id) REFERENCES import_items(id),
    CONSTRAINT fk_analysis_reports_match FOREIGN KEY (match_id) REFERENCES matches(id)
);
CREATE UNIQUE INDEX uk_analysis_reports_import ON analysis_reports(import_item_id, analysis_id);
CREATE INDEX idx_analysis_reports_match ON analysis_reports(match_id);

CREATE TABLE bets (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    import_item_id BIGINT NOT NULL,
    match_id BIGINT,
    bet_id VARCHAR(160) NOT NULL,
    match_name VARCHAR(300),
    market_type VARCHAR(160),
    selection_text VARCHAR(500),
    stake DECIMAL(14,4),
    odds DECIMAL(12,4),
    hit_status VARCHAR(80) NOT NULL DEFAULT 'PENDING',
    profit_loss DECIMAL(14,4),
    raw_payload LONGTEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bets_import_item FOREIGN KEY (import_item_id) REFERENCES import_items(id),
    CONSTRAINT fk_bets_match FOREIGN KEY (match_id) REFERENCES matches(id)
);
CREATE UNIQUE INDEX uk_bets_import_bet ON bets(import_item_id, bet_id);
CREATE INDEX idx_bets_match ON bets(match_id);

CREATE TABLE import_item_mappings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    import_item_id BIGINT NOT NULL,
    target_type VARCHAR(120) NOT NULL,
    target_id BIGINT NOT NULL,
    mapping_status VARCHAR(80) NOT NULL DEFAULT 'IMPORTED',
    message VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_import_item_mappings_item FOREIGN KEY (import_item_id) REFERENCES import_items(id)
);
CREATE INDEX idx_import_item_mappings_item ON import_item_mappings(import_item_id);
CREATE UNIQUE INDEX uk_import_item_mappings_target ON import_item_mappings(import_item_id, target_type, target_id);
