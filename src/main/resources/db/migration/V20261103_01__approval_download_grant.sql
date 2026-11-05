CREATE TABLE approval_download_grant (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    approval_id BIGINT UNSIGNED NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expires_at DATETIME(3) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    download_count BIGINT NOT NULL DEFAULT 0,
    last_downloaded_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_approval_download_grant_approval (approval_id),
    UNIQUE KEY uk_approval_download_grant_token (token_hash),
    KEY idx_approval_download_grant_expiry (status, expires_at),
    CONSTRAINT fk_approval_download_grant_approval
        FOREIGN KEY (approval_id) REFERENCES approval_request(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
