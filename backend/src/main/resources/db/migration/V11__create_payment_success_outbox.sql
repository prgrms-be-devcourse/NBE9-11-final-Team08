CREATE TABLE payment_success_outbox_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    payment_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    status ENUM ('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED') NOT NULL,
    last_error LONGTEXT,
    processed_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_success_outbox_payment (payment_id),
    KEY idx_payment_success_outbox_status_id (status, id),
    CONSTRAINT fk_payment_success_outbox_payment FOREIGN KEY (payment_id) REFERENCES payments (id),
    CONSTRAINT fk_payment_success_outbox_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB;
