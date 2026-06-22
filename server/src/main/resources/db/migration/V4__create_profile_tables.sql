CREATE TABLE collection_jobs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    source_type VARCHAR(120) NOT NULL,
    source_name VARCHAR(240) NOT NULL,
    keyword VARCHAR(500),
    status VARCHAR(80) NOT NULL DEFAULT 'PENDING',
    triggered_by VARCHAR(120),
    message VARCHAR(1000),
    total_items INT NOT NULL DEFAULT 0,
    pending_items INT NOT NULL DEFAULT 0,
    approved_items INT NOT NULL DEFAULT 0,
    rejected_items INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_collection_jobs_status ON collection_jobs(status);

CREATE TABLE collection_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    job_id BIGINT NOT NULL,
    entity_type VARCHAR(40) NOT NULL,
    entity_key VARCHAR(240) NOT NULL,
    fact_type VARCHAR(120) NOT NULL,
    period_key VARCHAR(120),
    title VARCHAR(300) NOT NULL,
    summary TEXT NOT NULL,
    sentiment_label VARCHAR(80),
    confidence_score DECIMAL(5,2),
    reliability_score DECIMAL(5,2),
    source_name VARCHAR(240) NOT NULL,
    source_url VARCHAR(1000),
    source_ref VARCHAR(500),
    captured_at TIMESTAMP NULL,
    raw_payload LONGTEXT,
    status VARCHAR(80) NOT NULL DEFAULT 'PENDING_REVIEW',
    review_note VARCHAR(1000),
    reviewed_by VARCHAR(120),
    reviewed_at TIMESTAMP NULL,
    target_type VARCHAR(120),
    target_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_collection_items_job FOREIGN KEY (job_id) REFERENCES collection_jobs(id)
);
CREATE INDEX idx_collection_items_status ON collection_items(status);
CREATE INDEX idx_collection_items_entity ON collection_items(entity_type, entity_key);

CREATE TABLE team_profile_facts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    team_id BIGINT NOT NULL,
    collection_item_id BIGINT,
    fact_type VARCHAR(120) NOT NULL,
    period_key VARCHAR(120),
    title VARCHAR(300) NOT NULL,
    summary TEXT NOT NULL,
    sentiment_label VARCHAR(80),
    confidence_score DECIMAL(5,2),
    reliability_score DECIMAL(5,2),
    source_name VARCHAR(240) NOT NULL,
    source_url VARCHAR(1000),
    source_ref VARCHAR(500),
    captured_at TIMESTAMP NULL,
    approved_by VARCHAR(120),
    raw_payload LONGTEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_team_profile_facts_team FOREIGN KEY (team_id) REFERENCES teams(id),
    CONSTRAINT fk_team_profile_facts_collection_item FOREIGN KEY (collection_item_id) REFERENCES collection_items(id)
);
CREATE INDEX idx_team_profile_facts_team ON team_profile_facts(team_id);
CREATE UNIQUE INDEX uk_team_profile_facts_collection_item ON team_profile_facts(collection_item_id);

CREATE TABLE player_profile_facts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_id BIGINT NOT NULL,
    collection_item_id BIGINT,
    fact_type VARCHAR(120) NOT NULL,
    period_key VARCHAR(120),
    title VARCHAR(300) NOT NULL,
    summary TEXT NOT NULL,
    sentiment_label VARCHAR(80),
    confidence_score DECIMAL(5,2),
    reliability_score DECIMAL(5,2),
    source_name VARCHAR(240) NOT NULL,
    source_url VARCHAR(1000),
    source_ref VARCHAR(500),
    captured_at TIMESTAMP NULL,
    approved_by VARCHAR(120),
    raw_payload LONGTEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_player_profile_facts_player FOREIGN KEY (player_id) REFERENCES players(id),
    CONSTRAINT fk_player_profile_facts_collection_item FOREIGN KEY (collection_item_id) REFERENCES collection_items(id)
);
CREATE INDEX idx_player_profile_facts_player ON player_profile_facts(player_id);
CREATE UNIQUE INDEX uk_player_profile_facts_collection_item ON player_profile_facts(collection_item_id);
