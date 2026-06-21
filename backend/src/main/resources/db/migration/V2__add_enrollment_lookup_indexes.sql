CREATE INDEX idx_enrollment_user_status_course
    ON enrollments (user_id, status, course_id);

CREATE INDEX idx_enrollment_order_status
    ON enrollments (order_id, status);
