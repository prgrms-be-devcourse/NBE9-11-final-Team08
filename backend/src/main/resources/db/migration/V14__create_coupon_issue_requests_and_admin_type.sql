CREATE TEMPORARY TABLE tmp_issued_coupon_keep AS
SELECT
    user_id,
    policy_id,
    MIN(id) AS keep_id
FROM issued_coupons
GROUP BY user_id, policy_id;

UPDATE order_coupon_usages ocu
JOIN issued_coupons ic ON ic.id = ocu.issued_coupon_id
JOIN tmp_issued_coupon_keep k
    ON k.user_id = ic.user_id
   AND k.policy_id = ic.policy_id
SET ocu.issued_coupon_id = k.keep_id
WHERE ocu.issued_coupon_id <> k.keep_id;

UPDATE coupon_reward_histories crh
JOIN issued_coupons ic ON ic.id = crh.issued_coupon_id
JOIN tmp_issued_coupon_keep k
    ON k.user_id = ic.user_id
   AND k.policy_id = ic.policy_id
SET crh.issued_coupon_id = k.keep_id
WHERE crh.issued_coupon_id <> k.keep_id;

DELETE ic
FROM issued_coupons ic
JOIN tmp_issued_coupon_keep k
    ON k.user_id = ic.user_id
   AND k.policy_id = ic.policy_id
WHERE ic.id <> k.keep_id;

DROP TEMPORARY TABLE tmp_issued_coupon_keep;

ALTER TABLE issued_coupons
    DROP INDEX uk_issued_coupon_user_policy_issue_key;

ALTER TABLE issued_coupons
    ADD UNIQUE KEY uk_issued_coupon_user_policy (user_id, policy_id);

CREATE TABLE coupon_issue_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    policy_id BIGINT NOT NULL,
    request_key VARCHAR(100) NOT NULL,
    issue_type ENUM ('SIGNUP', 'ATTENDANCE_STREAK', 'MONTHLY_ATTENDANCE', 'SELECTED_USERS', 'ALL_USERS') NOT NULL,
    status ENUM ('REQUESTED', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELED') NOT NULL,
    requested_count BIGINT NOT NULL DEFAULT 0,
    success_count BIGINT NOT NULL DEFAULT 0,
    failed_count BIGINT NOT NULL DEFAULT 0,
    skipped_count BIGINT NOT NULL DEFAULT 0,
    target_user_max_id BIGINT,
    requested_by BIGINT,
    requested_at DATETIME(6) NOT NULL,
    started_at DATETIME(6),
    completed_at DATETIME(6),
    failure_reason LONGTEXT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_coupon_issue_request_key (issue_type, request_key),
    INDEX idx_coupon_issue_requests_status_id (status, id),
    INDEX idx_coupon_issue_requests_policy_id (policy_id, id),
    INDEX idx_coupon_issue_requests_requested_by (requested_by, id),
    INDEX idx_coupon_issue_requests_all_grants (issue_type, status, target_user_max_id, policy_id, id)
) ENGINE=InnoDB;

ALTER TABLE coupon_policies
    MODIFY COLUMN coupon_type ENUM ('AUTO', 'FCFS', 'NORMAL', 'ADMIN_ISSUE') NOT NULL;
