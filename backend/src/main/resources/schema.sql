CREATE TABLE IF NOT EXISTS student_profiles (
    student_id VARCHAR(32) NOT NULL,
    student_name VARCHAR(64) NOT NULL,
    gender VARCHAR(16) DEFAULT NULL,
    college VARCHAR(128) DEFAULT NULL,
    grade VARCHAR(32) DEFAULT NULL,
    major VARCHAR(128) DEFAULT NULL,
    class_name VARCHAR(128) DEFAULT NULL,
    PRIMARY KEY (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS student_accounts (
    student_id VARCHAR(32) NOT NULL,
    password_hash CHAR(64) NOT NULL,
    password_salt VARCHAR(64) NOT NULL,
    must_change_password TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (student_id),
    CONSTRAINT fk_student_accounts_profile
        FOREIGN KEY (student_id) REFERENCES student_profiles (student_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS leave_applications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    student_id VARCHAR(32) NOT NULL,
    student_name VARCHAR(64) NOT NULL,
    is_outside_jinzhong TINYINT(1) DEFAULT NULL,
    leave_type VARCHAR(64) DEFAULT NULL,
    address_levels JSON DEFAULT NULL,
    detailed_address VARCHAR(255) DEFAULT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    duration_minutes INT DEFAULT NULL,
    reason TEXT,
    attachments_json JSON DEFAULT NULL,
    student_phone VARCHAR(32) DEFAULT NULL,
    emergency_contact_name VARCHAR(64) DEFAULT NULL,
    emergency_contact_phone VARCHAR(32) DEFAULT NULL,
    approval_level INT DEFAULT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'pending',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_leave_student_created (student_id, created_at),
    KEY idx_leave_status_end_time (status, end_time),
    CONSTRAINT fk_leave_student_profile
        FOREIGN KEY (student_id) REFERENCES student_profiles (student_id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS approval_steps (
    id BIGINT NOT NULL AUTO_INCREMENT,
    leave_id BIGINT NOT NULL,
    step_no INT NOT NULL,
    approver_role VARCHAR(64) NOT NULL,
    approver_name VARCHAR(64) DEFAULT NULL,
    approver_code VARCHAR(64) DEFAULT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'waiting',
    comment VARCHAR(255) DEFAULT NULL,
    decided_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_approval_leave_step (leave_id, step_no),
    KEY idx_approval_status_role (status, approver_role),
    CONSTRAINT fk_approval_leave
        FOREIGN KEY (leave_id) REFERENCES leave_applications (id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS return_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    leave_id BIGINT NOT NULL,
    student_id VARCHAR(32) NOT NULL,
    return_time DATETIME DEFAULT NULL,
    note VARCHAR(255) DEFAULT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'pending',
    comment VARCHAR(255) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_return_leave_created (leave_id, created_at),
    KEY idx_return_status (status),
    CONSTRAINT fk_return_leave
        FOREIGN KEY (leave_id) REFERENCES leave_applications (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_return_student_profile
        FOREIGN KEY (student_id) REFERENCES student_profiles (student_id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS return_extension_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    leave_id BIGINT NOT NULL,
    student_id VARCHAR(32) NOT NULL,
    old_return_time DATETIME DEFAULT NULL,
    new_return_time DATETIME NOT NULL,
    reason TEXT,
    approval_level INT NOT NULL DEFAULT 1,
    current_step INT NOT NULL DEFAULT 1,
    status VARCHAR(32) NOT NULL DEFAULT 'pending',
    comment VARCHAR(255) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_extension_leave_created (leave_id, created_at),
    KEY idx_extension_status_step (status, current_step),
    CONSTRAINT fk_extension_leave
        FOREIGN KEY (leave_id) REFERENCES leave_applications (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_extension_student_profile
        FOREIGN KEY (student_id) REFERENCES student_profiles (student_id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS admin_role_settings (
    id INT NOT NULL,
    current_role VARCHAR(64) NOT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS admin_accounts (
    admin_id VARCHAR(32) NOT NULL,
    admin_name VARCHAR(64) NOT NULL,
    password_hash CHAR(64) NOT NULL,
    password_salt VARCHAR(64) NOT NULL,
    role VARCHAR(64) NOT NULL DEFAULT '辅导员',
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (admin_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS admin_cc_records (
    id BIGINT NOT NULL AUTO_INCREMENT,
    leave_id BIGINT NOT NULL,
    receiver_role VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_cc_receiver_created (receiver_role, created_at),
    CONSTRAINT fk_cc_leave
        FOREIGN KEY (leave_id) REFERENCES leave_applications (id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
