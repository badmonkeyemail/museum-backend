CREATE TABLE IF NOT EXISTS app_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS app_user_role (
    user_id BIGINT NOT NULL,
    role_code VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id, role_code),
    CONSTRAINT fk_app_user_role_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
    CONSTRAINT chk_app_user_role_code CHECK (role_code IN ('SUPER', 'ADMIN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- A single fixed row is the mutex for the system-wide single-user login rule.
CREATE TABLE IF NOT EXISTS app_active_session (
    id TINYINT NOT NULL,
    user_id BIGINT NULL,
    token_hash CHAR(64) NULL,
    issued_at DATETIME(6) NULL,
    last_seen_at DATETIME(6) NULL,
    expires_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_app_active_session_singleton CHECK (id = 1),
    CONSTRAINT fk_app_active_session_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO app_active_session (id) VALUES (1)
ON DUPLICATE KEY UPDATE id = VALUES(id);
