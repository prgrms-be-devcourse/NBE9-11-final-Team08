ALTER TABLE issued_coupons
    DROP INDEX uk_issued_coupon_user_policy;

ALTER TABLE issued_coupons
    ADD COLUMN issue_key VARCHAR(100) NOT NULL DEFAULT 'DOWNLOAD' AFTER user_id;

ALTER TABLE issued_coupons
    ADD UNIQUE KEY uk_issued_coupon_user_policy_issue_key (user_id, policy_id, issue_key);

ALTER TABLE coupon_policies
    ADD COLUMN auto_issue_type ENUM ('ATTENDANCE_STREAK', 'MONTHLY_ATTENDANCE', 'SIGNUP') AFTER coupon_type;

CREATE TABLE coupon_reward_histories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    policy_id BIGINT NOT NULL,
    reward_key VARCHAR(100) NOT NULL,
    reward_type VARCHAR(50) NOT NULL,
    issued_coupon_id BIGINT,
    issued_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_coupon_reward_history_user_reward (user_id, reward_key),
    INDEX idx_coupon_reward_histories_user (user_id),
    INDEX idx_coupon_reward_histories_policy (policy_id)
) ENGINE=InnoDB;

CREATE TABLE coupon_reward_outbox_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_key VARCHAR(100) NOT NULL,
    payload LONGTEXT NOT NULL,
    status ENUM ('DEAD', 'FAILED', 'PENDING', 'PROCESSED') NOT NULL,
    retry_count INT NOT NULL,
    last_error LONGTEXT,
    processed_at DATETIME(6),
    next_retry_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_coupon_reward_outbox_event_key (event_type, event_key),
    INDEX idx_coupon_reward_outbox_status_id (status, id),
    INDEX idx_coupon_reward_outbox_user (user_id, id)
) ENGINE=InnoDB;
