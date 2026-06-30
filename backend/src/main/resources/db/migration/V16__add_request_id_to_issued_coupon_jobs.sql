ALTER TABLE issued_coupon_jobs
ADD COLUMN request_id VARCHAR(36);

UPDATE issued_coupon_jobs
SET request_id = UUID()
WHERE request_id IS NULL;

ALTER TABLE issued_coupon_jobs
MODIFY COLUMN request_id VARCHAR(36) NOT NULL;

ALTER TABLE issued_coupon_jobs
ADD CONSTRAINT uk_issued_coupon_jobs_request_id UNIQUE (request_id);
