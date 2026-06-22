ALTER TABLE payments
    ADD COLUMN provider ENUM ('MOCK', 'TOSS', 'NICEPAY', 'KCP') NOT NULL DEFAULT 'MOCK',
    ADD COLUMN idempotency_key VARCHAR(255),
    ADD COLUMN failure_code VARCHAR(255),
    ADD COLUMN failure_message VARCHAR(255);

CREATE TABLE payment_attempts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    payment_id BIGINT NOT NULL,
    provider ENUM ('MOCK', 'TOSS', 'NICEPAY', 'KCP') NOT NULL,
    status ENUM ('REQUESTED', 'SUCCESS', 'FAILED') NOT NULL,
    amount INT NOT NULL,
    payment_key VARCHAR(255),
    failure_code VARCHAR(255),
    failure_message VARCHAR(255),
    requested_at DATETIME(6) NOT NULL,
    completed_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_payment_attempt_payment FOREIGN KEY (payment_id) REFERENCES payments (id)
) ENGINE=InnoDB;

CREATE INDEX idx_payment_attempt_payment_created_at
    ON payment_attempts (payment_id, created_at);

CREATE INDEX idx_payment_attempt_provider_status
    ON payment_attempts (provider, status);
