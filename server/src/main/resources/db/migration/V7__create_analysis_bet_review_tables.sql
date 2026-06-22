CREATE TABLE bet_plans (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    import_item_id BIGINT NOT NULL,
    analysis_report_id BIGINT,
    match_id BIGINT,
    plan_key VARCHAR(160) NOT NULL,
    plan_title VARCHAR(300) NOT NULL,
    conclusion_type VARCHAR(160),
    confidence VARCHAR(80),
    budget_amount DECIMAL(14,4),
    risk_summary TEXT,
    betting_method VARCHAR(160),
    strategy_type VARCHAR(160),
    status VARCHAR(80) NOT NULL DEFAULT 'IMPORTED',
    generated_by VARCHAR(160),
    generated_at TIMESTAMP NULL,
    raw_payload LONGTEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bet_plans_import_item FOREIGN KEY (import_item_id) REFERENCES import_items(id),
    CONSTRAINT fk_bet_plans_analysis_report FOREIGN KEY (analysis_report_id) REFERENCES analysis_reports(id),
    CONSTRAINT fk_bet_plans_match FOREIGN KEY (match_id) REFERENCES matches(id)
);
CREATE UNIQUE INDEX uk_bet_plans_import_key ON bet_plans(import_item_id, plan_key);
CREATE INDEX idx_bet_plans_match ON bet_plans(match_id);
CREATE INDEX idx_bet_plans_method ON bet_plans(betting_method);

CREATE TABLE bet_plan_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    bet_plan_id BIGINT NOT NULL,
    match_id BIGINT,
    market_type VARCHAR(160),
    selection_text VARCHAR(500) NOT NULL,
    stake_suggestion DECIMAL(14,4),
    odds DECIMAL(12,4),
    line_value VARCHAR(120),
    logic_type VARCHAR(120),
    risk_level VARCHAR(80),
    play_type VARCHAR(160),
    pass_type VARCHAR(160),
    item_order INT NOT NULL DEFAULT 0,
    raw_payload LONGTEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bet_plan_items_plan FOREIGN KEY (bet_plan_id) REFERENCES bet_plans(id),
    CONSTRAINT fk_bet_plan_items_match FOREIGN KEY (match_id) REFERENCES matches(id)
);
CREATE INDEX idx_bet_plan_items_plan ON bet_plan_items(bet_plan_id);
CREATE INDEX idx_bet_plan_items_match ON bet_plan_items(match_id);
CREATE INDEX idx_bet_plan_items_market ON bet_plan_items(market_type);

CREATE TABLE post_match_reviews (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    import_item_id BIGINT NOT NULL,
    match_id BIGINT,
    analysis_report_id BIGINT,
    review_key VARCHAR(160) NOT NULL,
    review_title VARCHAR(300) NOT NULL,
    math_review TEXT,
    football_review TEXT,
    handicap_review TEXT,
    tournament_temperament_review TEXT,
    odds_value_review TEXT,
    overall_summary TEXT,
    raw_payload LONGTEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_post_match_reviews_import_item FOREIGN KEY (import_item_id) REFERENCES import_items(id),
    CONSTRAINT fk_post_match_reviews_match FOREIGN KEY (match_id) REFERENCES matches(id),
    CONSTRAINT fk_post_match_reviews_analysis_report FOREIGN KEY (analysis_report_id) REFERENCES analysis_reports(id)
);
CREATE UNIQUE INDEX uk_post_match_reviews_import_key ON post_match_reviews(import_item_id, review_key);
CREATE INDEX idx_post_match_reviews_match ON post_match_reviews(match_id);

CREATE TABLE review_lessons (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    review_id BIGINT NOT NULL,
    lesson_type VARCHAR(120) NOT NULL DEFAULT 'GENERAL',
    lesson_text TEXT NOT NULL,
    severity VARCHAR(80) NOT NULL DEFAULT 'INFO',
    raw_payload LONGTEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_review_lessons_review FOREIGN KEY (review_id) REFERENCES post_match_reviews(id)
);
CREATE INDEX idx_review_lessons_review ON review_lessons(review_id);
CREATE INDEX idx_review_lessons_type ON review_lessons(lesson_type);

ALTER TABLE bets ADD COLUMN ticket_no VARCHAR(160);
ALTER TABLE bets ADD COLUMN bet_date DATE;
ALTER TABLE bets ADD COLUMN matchday DATE;
ALTER TABLE bets ADD COLUMN closing_odds DECIMAL(12,4);
ALTER TABLE bets ADD COLUMN clv DECIMAL(12,6);
ALTER TABLE bets ADD COLUMN return_amount DECIMAL(14,4);
ALTER TABLE bets ADD COLUMN settled_at TIMESTAMP NULL;
ALTER TABLE bets ADD COLUMN plan_item_id BIGINT;
ALTER TABLE bets ADD COLUMN review_status VARCHAR(80) NOT NULL DEFAULT 'UNREVIEWED';
ALTER TABLE bets ADD CONSTRAINT fk_bets_plan_item FOREIGN KEY (plan_item_id) REFERENCES bet_plan_items(id);
CREATE INDEX idx_bets_ticket_no ON bets(ticket_no);
CREATE INDEX idx_bets_bet_date ON bets(bet_date);
CREATE INDEX idx_bets_review_status ON bets(review_status);
