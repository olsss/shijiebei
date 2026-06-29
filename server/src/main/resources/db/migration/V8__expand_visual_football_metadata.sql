ALTER TABLE teams ADD COLUMN country_iso2 VARCHAR(2);
ALTER TABLE teams ADD COLUMN flag_asset_key VARCHAR(120);
ALTER TABLE teams ADD COLUMN confederation VARCHAR(80);
ALTER TABLE teams ADD COLUMN group_name VARCHAR(80);
ALTER TABLE teams ADD COLUMN metadata_source_ref VARCHAR(500);

CREATE INDEX idx_teams_country_iso2 ON teams(country_iso2);
CREATE INDEX idx_teams_confederation ON teams(confederation);
CREATE INDEX idx_teams_group_name ON teams(group_name);

CREATE TABLE team_metric_snapshots (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    import_item_id BIGINT,
    team_id BIGINT NOT NULL,
    match_id BIGINT,
    period_key VARCHAR(120),
    metric_type VARCHAR(120) NOT NULL DEFAULT 'RAW',
    xg DECIMAL(10,4),
    xga DECIMAL(10,4),
    npxg DECIMAL(10,4),
    ppda DECIMAL(10,4),
    xpts DECIMAL(10,4),
    shots INT,
    shots_on_target INT,
    possession_pct DECIMAL(8,4),
    progressive_passes INT,
    set_piece_xg DECIMAL(10,4),
    form_score DECIMAL(8,4),
    source_name VARCHAR(240) NOT NULL DEFAULT 'UNKNOWN',
    source_ref VARCHAR(500),
    captured_at TIMESTAMP NULL,
    raw_payload LONGTEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_team_metrics_import_item FOREIGN KEY (import_item_id) REFERENCES import_items(id),
    CONSTRAINT fk_team_metrics_team FOREIGN KEY (team_id) REFERENCES teams(id),
    CONSTRAINT fk_team_metrics_match FOREIGN KEY (match_id) REFERENCES matches(id)
);
CREATE INDEX idx_team_metrics_team ON team_metric_snapshots(team_id);
CREATE INDEX idx_team_metrics_match ON team_metric_snapshots(match_id);
CREATE INDEX idx_team_metrics_type ON team_metric_snapshots(metric_type);
CREATE INDEX idx_team_metrics_captured ON team_metric_snapshots(captured_at);

CREATE TABLE player_metric_snapshots (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    import_item_id BIGINT,
    player_id BIGINT NOT NULL,
    team_id BIGINT,
    match_id BIGINT,
    period_key VARCHAR(120),
    metric_type VARCHAR(120) NOT NULL DEFAULT 'RAW',
    minutes_played INT,
    goals DECIMAL(10,4),
    assists DECIMAL(10,4),
    xg DECIMAL(10,4),
    xa DECIMAL(10,4),
    npxg DECIMAL(10,4),
    shots INT,
    shots_on_target INT,
    key_passes INT,
    progressive_passes INT,
    training_load DECIMAL(10,4),
    availability_score DECIMAL(8,4),
    expected_starting_probability DECIMAL(8,6),
    source_name VARCHAR(240) NOT NULL DEFAULT 'UNKNOWN',
    source_ref VARCHAR(500),
    captured_at TIMESTAMP NULL,
    raw_payload LONGTEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_player_metrics_import_item FOREIGN KEY (import_item_id) REFERENCES import_items(id),
    CONSTRAINT fk_player_metrics_player FOREIGN KEY (player_id) REFERENCES players(id),
    CONSTRAINT fk_player_metrics_team FOREIGN KEY (team_id) REFERENCES teams(id),
    CONSTRAINT fk_player_metrics_match FOREIGN KEY (match_id) REFERENCES matches(id)
);
CREATE INDEX idx_player_metrics_player ON player_metric_snapshots(player_id);
CREATE INDEX idx_player_metrics_team ON player_metric_snapshots(team_id);
CREATE INDEX idx_player_metrics_match ON player_metric_snapshots(match_id);
CREATE INDEX idx_player_metrics_type ON player_metric_snapshots(metric_type);

CREATE TABLE match_market_signals (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    import_item_id BIGINT,
    match_id BIGINT NOT NULL,
    market_code VARCHAR(120) NOT NULL DEFAULT 'RAW',
    bookmaker VARCHAR(240),
    opening_line VARCHAR(120),
    current_line VARCHAR(120),
    opening_odds DECIMAL(12,4),
    current_odds DECIMAL(12,4),
    implied_probability DECIMAL(12,6),
    public_bet_pct DECIMAL(8,4),
    money_pct DECIMAL(8,4),
    movement_direction VARCHAR(80) NOT NULL DEFAULT 'UNKNOWN',
    signal_level VARCHAR(80) NOT NULL DEFAULT 'INFO',
    source_name VARCHAR(240) NOT NULL DEFAULT 'UNKNOWN',
    source_ref VARCHAR(500),
    observed_at TIMESTAMP NULL,
    raw_payload LONGTEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_market_signals_import_item FOREIGN KEY (import_item_id) REFERENCES import_items(id),
    CONSTRAINT fk_market_signals_match FOREIGN KEY (match_id) REFERENCES matches(id)
);
CREATE INDEX idx_market_signals_match ON match_market_signals(match_id);
CREATE INDEX idx_market_signals_market ON match_market_signals(market_code);
CREATE INDEX idx_market_signals_observed ON match_market_signals(observed_at);
