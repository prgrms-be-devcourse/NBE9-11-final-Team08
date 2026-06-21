CREATE TABLE issued_coupon_jobs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    policy_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status ENUM ('REQUESTED', 'RETRYING', 'ISSUED', 'DEAD') NOT NULL,
    failure_reason VARCHAR(500),
    retry_count INT NOT NULL DEFAULT 0,
    requested_at DATETIME(6) NOT NULL,
    last_tried_at DATETIME(6),
    completed_at DATETIME(6),
    PRIMARY KEY (id),
    INDEX idx_issued_coupon_jobs_policy_user (policy_id, user_id),
    INDEX idx_issued_coupon_jobs_status (status)
) ENGINE=InnoDB;
