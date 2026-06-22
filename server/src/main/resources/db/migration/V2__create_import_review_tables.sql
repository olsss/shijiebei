CREATE TABLE import_jobs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    archive_path VARCHAR(1000) NOT NULL,
    status VARCHAR(40) NOT NULL,
    total_items INT NOT NULL DEFAULT 0,
    valid_items INT NOT NULL DEFAULT 0,
    invalid_items INT NOT NULL DEFAULT 0,
    message VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE import_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    job_id BIGINT NOT NULL,
    item_type VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    relative_path VARCHAR(700) NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    summary_title VARCHAR(500) NOT NULL,
    valid_json BOOLEAN NOT NULL,
    validation_message VARCHAR(1000) NOT NULL,
    raw_json LONGTEXT NOT NULL,
    rejection_reason VARCHAR(1000),
    reviewed_by VARCHAR(120),
    reviewed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_import_items_job FOREIGN KEY (job_id) REFERENCES import_jobs(id),
    CONSTRAINT uk_import_items_job_path UNIQUE (job_id, relative_path)
);
