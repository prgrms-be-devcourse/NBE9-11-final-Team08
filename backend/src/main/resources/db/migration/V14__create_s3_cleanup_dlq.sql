CREATE TABLE s3_cleanup_dlq (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    lecture_id      BIGINT NOT NULL,
    target_dir_name VARCHAR(255) NOT NULL,
    status          ENUM ('DEAD', 'PENDING') NOT NULL DEFAULT 'PENDING',
    retry_count     INT NOT NULL DEFAULT 0,
    last_error      TEXT,
    failed_at       DATETIME(6) NOT NULL,
    next_retry_at   DATETIME(6),
    created_at      DATETIME(6) NOT NULL,
    updated_at      DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_s3_cleanup_dlq_status_retry (status, next_retry_at)
) ENGINE = InnoDB;
