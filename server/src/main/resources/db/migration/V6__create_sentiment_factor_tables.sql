CREATE TABLE match_context_factors (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    import_item_id BIGINT NOT NULL,
    match_id BIGINT,
    factor_category VARCHAR(120) NOT NULL DEFAULT 'OTHER',
    factor_type VARCHAR(120) NOT NULL DEFAULT 'RAW',
    title VARCHAR(300) NOT NULL,
    summary TEXT NOT NULL,
    impact_direction VARCHAR(80) NOT NULL DEFAULT 'UNKNOWN',
    entity_type VARCHAR(80) NOT NULL DEFAULT 'MATCH',
    entity_key VARCHAR(240),
    evidence_level VARCHAR(80) NOT NULL DEFAULT 'UNKNOWN',
    source_name VARCHAR(240) NOT NULL DEFAULT 'UNKNOWN',
    source_url VARCHAR(1000),
    source_ref VARCHAR(500),
    observed_at TIMESTAMP NULL,
    expires_at TIMESTAMP NULL,
    confidence_score DECIMAL(5,2),
    reliability_score DECIMAL(5,2),
    raw_payload LONGTEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_context_factor_import_item FOREIGN KEY (import_item_id) REFERENCES import_items(id),
    CONSTRAINT fk_context_factor_match FOREIGN KEY (match_id) REFERENCES matches(id)
);
CREATE INDEX idx_context_factor_import_item ON match_context_factors(import_item_id);
CREATE INDEX idx_context_factor_match ON match_context_factors(match_id);
CREATE INDEX idx_context_factor_category ON match_context_factors(factor_category);
CREATE INDEX idx_context_factor_expires ON match_context_factors(expires_at);

CREATE TABLE sentiment_risk_assessments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    import_item_id BIGINT NOT NULL,
    match_id BIGINT,
    factor_id BIGINT,
    risk_type VARCHAR(120) NOT NULL DEFAULT 'RAW_RISK',
    risk_level VARCHAR(80) NOT NULL DEFAULT 'UNKNOWN',
    risk_score DECIMAL(8,4),
    title VARCHAR(300) NOT NULL,
    rationale TEXT NOT NULL,
    suggested_action VARCHAR(120) NOT NULL DEFAULT 'MONITOR',
    source_name VARCHAR(240) NOT NULL DEFAULT 'UNKNOWN',
    source_ref VARCHAR(500),
    raw_payload LONGTEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sentiment_risk_import_item FOREIGN KEY (import_item_id) REFERENCES import_items(id),
    CONSTRAINT fk_sentiment_risk_match FOREIGN KEY (match_id) REFERENCES matches(id),
    CONSTRAINT fk_sentiment_risk_factor FOREIGN KEY (factor_id) REFERENCES match_context_factors(id)
);
CREATE INDEX idx_sentiment_risk_import_item ON sentiment_risk_assessments(import_item_id);
CREATE INDEX idx_sentiment_risk_match ON sentiment_risk_assessments(match_id);
CREATE INDEX idx_sentiment_risk_factor ON sentiment_risk_assessments(factor_id);
CREATE INDEX idx_sentiment_risk_type ON sentiment_risk_assessments(risk_type);
