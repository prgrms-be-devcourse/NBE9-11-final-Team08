CREATE TABLE ai_feedbacks (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    study_id BIGINT NOT NULL,
    study_activity_id BIGINT NOT NULL,
    status ENUM ('COMPLETED', 'FAILED', 'PENDING', 'PROCESSING', 'STALE') NOT NULL,
    feedback LONGTEXT,
    activity_content_snapshot TINYTEXT NOT NULL,
    model_name VARCHAR(255) NOT NULL,
    prompt_version VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_feedback_study_activity (study_activity_id)
) ENGINE=InnoDB;

CREATE TABLE attendances (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    attendance_date DATE NOT NULL,
    consecutive_days INT NOT NULL,
    monthly_total_days INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_attendance_user_date (user_id, attendance_date)
) ENGINE=InnoDB;

CREATE TABLE cart_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cart_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_cart_item_cart_course (cart_id, course_id)
) ENGINE=InnoDB;

CREATE TABLE carts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_cart_user (user_id)
) ENGINE=InnoDB;

CREATE TABLE categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    parent_category_id BIGINT,
    name VARCHAR(255) NOT NULL,
    depth INT NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE coupon_policies (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    discount_type ENUM ('AMOUNT', 'PERCENT') NOT NULL,
    discount_value INT NOT NULL,
    max_discount_amount INT,
    valid_days INT NOT NULL,
    total_quantity INT,
    category_id BIGINT,
    coupon_type ENUM ('AUTO', 'FCFS', 'NORMAL') NOT NULL,
    coupon_target ENUM ('ALL', 'CATEGORY', 'COURSE') NOT NULL,
    usage_type ENUM ('MULTI_USE', 'SINGLE_USE') NOT NULL,
    is_stackable BIT NOT NULL,
    issue_start_date DATETIME(6),
    issue_end_date DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE course_status_histories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    from_status ENUM ('DELETED', 'DRAFT', 'IN_REVIEW', 'ON_SALE', 'SUSPENDED'),
    to_status ENUM ('DELETED', 'DRAFT', 'IN_REVIEW', 'ON_SALE', 'SUSPENDED'),
    reason VARCHAR(1000),
    changed_by BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE courses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    instructor_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    thumbnail VARCHAR(255) NOT NULL,
    price INT NOT NULL,
    status ENUM ('DELETED', 'DRAFT', 'IN_REVIEW', 'ON_SALE', 'SUSPENDED') NOT NULL,
    view_count INT NOT NULL,
    deleted_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE chapters (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    order_no INT NOT NULL,
    deleted_at DATETIME(6),
    course_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_chapter_course FOREIGN KEY (course_id) REFERENCES courses (id)
) ENGINE=InnoDB;

CREATE TABLE enrollments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    status ENUM ('ACTIVE', 'CANCELED', 'EXPIRED') NOT NULL,
    enrolled_at DATETIME(6) NOT NULL,
    canceled_at DATETIME(6),
    expired_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_enrollment_user_course (user_id, course_id)
) ENGINE=InnoDB;

CREATE TABLE issued_coupons (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    policy_id BIGINT NOT NULL,
    status ENUM ('EXPIRED', 'ISSUED', 'USED') NOT NULL,
    issued_at DATETIME(6) NOT NULL,
    expired_at DATETIME(6) NOT NULL,
    used_at DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_issued_coupon_user_policy (user_id, policy_id)
) ENGINE=InnoDB;

CREATE TABLE learning_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    course_id BIGINT,
    chapter_id BIGINT,
    lecture_id BIGINT,
    event_type ENUM ('LECTURE_COMPLETE', 'LECTURE_ENTER', 'POSITION_SAVE', 'VIDEO_END', 'VIDEO_START') NOT NULL,
    position_seconds INT,
    event_time DATETIME(6) NOT NULL,
    unique_event_key VARCHAR(255),
    PRIMARY KEY (id),
    UNIQUE KEY uk_learning_event_key (unique_event_key)
) ENGINE=InnoDB;

CREATE TABLE lecture_progresses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    lecture_id BIGINT NOT NULL,
    last_position_seconds INT NOT NULL,
    watched_seconds INT NOT NULL,
    progress_rate DECIMAL(5, 2),
    completed BIT NOT NULL,
    completed_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_lecture_progress_user_lecture (user_id, lecture_id)
) ENGINE=InnoDB;

CREATE TABLE lecture_reflections (
    id BIGINT NOT NULL AUTO_INCREMENT,
    lecture_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TINYTEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_lecture_reflection_user_lecture (user_id, lecture_id)
) ENGINE=InnoDB;

CREATE TABLE lectures (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    summary VARCHAR(255),
    duration_seconds INT NOT NULL,
    order_no INT NOT NULL,
    is_free_preview BIT NOT NULL,
    m3u8path VARCHAR(255) NOT NULL,
    deleted_at DATETIME(6),
    chapter_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_lecture_chapter FOREIGN KEY (chapter_id) REFERENCES chapters (id)
) ENGINE=InnoDB;

CREATE TABLE lecture_modification_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    lecture_id BIGINT NOT NULL,
    instructor_id BIGINT NOT NULL,
    beforem3u8path VARCHAR(255) NOT NULL,
    afterm3u8path VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    status ENUM ('APPROVED', 'PENDING', 'REJECTED') NOT NULL,
    rejected_reason TEXT,
    managed_by BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_lecture_modification_lecture FOREIGN KEY (lecture_id) REFERENCES lectures (id)
) ENGINE=InnoDB;

CREATE TABLE order_coupon_usages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    issued_coupon_id BIGINT NOT NULL,
    discount_amount INT NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE order_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    course_title VARCHAR(255) NOT NULL,
    price INT NOT NULL,
    discount_price INT NOT NULL,
    final_price INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    order_number VARCHAR(255) NOT NULL,
    total_price INT NOT NULL,
    discount_price INT NOT NULL,
    final_price INT NOT NULL,
    status ENUM ('CANCELED', 'EXPIRED', 'PAID', 'PENDING_PAYMENT', 'REFUNDED') NOT NULL,
    ordered_at DATETIME(6) NOT NULL,
    paid_at DATETIME(6),
    canceled_at DATETIME(6),
    refunded_at DATETIME(6),
    expired_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_number (order_number)
) ENGINE=InnoDB;

CREATE TABLE payments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    payment_key VARCHAR(255),
    method VARCHAR(255),
    amount INT NOT NULL,
    status ENUM ('CANCELED', 'FAILED', 'READY', 'REFUNDED', 'SUCCESS') NOT NULL,
    paid_at DATETIME(6),
    failed_reason VARCHAR(255),
    canceled_at DATETIME(6),
    refunded_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_order (order_id)
) ENGINE=InnoDB;

CREATE TABLE qna_answers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    question_id BIGINT NOT NULL,
    instructor_id BIGINT NOT NULL,
    content TINYTEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_qna_answer_question (question_id)
) ENGINE=InnoDB;

CREATE TABLE qna_questions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    lecture_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TINYTEXT NOT NULL,
    deleted_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE seller (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_seller_user (user_id)
) ENGINE=InnoDB;

CREATE TABLE study_activities (
    id BIGINT NOT NULL AUTO_INCREMENT,
    study_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    content TINYTEXT NOT NULL,
    deleted_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE study_reports (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    study_id BIGINT NOT NULL,
    total_watch_time INT,
    total_qna_count INT,
    progress_rate DECIMAL(5, 2),
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(255) NOT NULL,
    profile_image VARCHAR(255),
    role ENUM ('ROLE_ADMIN', 'ROLE_SELLER', 'ROLE_USER') NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_email (email)
) ENGINE=InnoDB;

CREATE TABLE refresh_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT,
    token_hash VARCHAR(255) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    revoked_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_token_hash (token_hash),
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE studies (
    id BIGINT NOT NULL AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status ENUM ('ACTIVE', 'DRAFT', 'INACTIVE', 'READONLY') NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_study_course (course_id),
    CONSTRAINT fk_study_course FOREIGN KEY (course_id) REFERENCES courses (id),
    CONSTRAINT fk_study_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE study_members (
    id BIGINT NOT NULL AUTO_INCREMENT,
    study_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role ENUM ('MEMBER', 'OWNER') NOT NULL,
    status ENUM ('ACTIVE', 'KICKED', 'LEFT') NOT NULL,
    joined_at DATETIME(6) NOT NULL,
    left_at DATETIME(6),
    kicked_at DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_study_member_study_user (study_id, user_id),
    CONSTRAINT fk_study_member_study FOREIGN KEY (study_id) REFERENCES studies (id),
    CONSTRAINT fk_study_member_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;
