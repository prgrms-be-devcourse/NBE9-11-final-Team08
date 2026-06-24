ALTER TABLE payment_attempts
    ADD COLUMN idempotency_key VARCHAR(255);

ALTER TABLE payment_attempts
    ADD CONSTRAINT uk_payment_attempt_payment_id_idempotency_key
        UNIQUE (payment_id, idempotency_key);
