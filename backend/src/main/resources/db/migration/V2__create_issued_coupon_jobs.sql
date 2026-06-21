CREATE TABLE issued_coupon_jobs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    policy_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status ENUM ('REQUESTED', 'ISSUED', 'FAILED') NOT NULL,
    failure_reason VARCHAR(500),
    requested_at DATETIME(6) NOT NULL,
    completed_at DATETIME(6),
    PRIMARY KEY (id),
    INDEX idx_issued_coupon_jobs_policy_user (policy_id, user_id),
    INDEX idx_issued_coupon_jobs_status (status)
) ENGINE=InnoDB;
