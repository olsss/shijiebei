CREATE TABLE odds_market_snapshots (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    import_item_id BIGINT NOT NULL,
    odds_snapshot_id BIGINT,
    match_id BIGINT,
    bookmaker VARCHAR(240) NOT NULL,
    market_code VARCHAR(120) NOT NULL DEFAULT 'RAW',
    market_name VARCHAR(240),
    snapshot_type VARCHAR(80) NOT NULL DEFAULT 'RAW',
    handicap_line DECIMAL(10,4),
    line_value VARCHAR(120),
    captured_at TIMESTAMP NULL,
    source_ref VARCHAR(500),
    raw_payload LONGTEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_odds_market_import_item FOREIGN KEY (import_item_id) REFERENCES import_items(id),
    CONSTRAINT fk_odds_market_snapshot FOREIGN KEY (odds_snapshot_id) REFERENCES odds_snapshots(id),
    CONSTRAINT fk_odds_market_match FOREIGN KEY (match_id) REFERENCES matches(id)
);
CREATE INDEX idx_odds_market_import_item ON odds_market_snapshots(import_item_id);
CREATE INDEX idx_odds_market_match ON odds_market_snapshots(match_id);
CREATE INDEX idx_odds_market_bookmaker ON odds_market_snapshots(bookmaker);
CREATE INDEX idx_odds_market_code ON odds_market_snapshots(market_code);
CREATE INDEX idx_odds_market_captured ON odds_market_snapshots(captured_at);

CREATE TABLE odds_selection_snapshots (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    market_snapshot_id BIGINT NOT NULL,
    selection_code VARCHAR(160) NOT NULL,
    selection_name VARCHAR(240) NOT NULL,
    odds_value DECIMAL(12,4),
    implied_probability DECIMAL(12,6),
    selection_status VARCHAR(80) NOT NULL DEFAULT 'UNKNOWN',
    raw_payload LONGTEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_odds_selection_market FOREIGN KEY (market_snapshot_id) REFERENCES odds_market_snapshots(id)
);
CREATE INDEX idx_odds_selection_market ON odds_selection_snapshots(market_snapshot_id);
CREATE INDEX idx_odds_selection_code ON odds_selection_snapshots(selection_code);
