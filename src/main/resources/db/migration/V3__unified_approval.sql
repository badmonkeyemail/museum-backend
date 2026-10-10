-- V3: unified approval model
-- Replaces the legacy outbound_approval table with:
-- approval_request + approval_item + approval_history.
-- Approval history is append-only audit data and is not used for state decisions.

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS approval_history;
DROP TABLE IF EXISTS approval_item;
DROP TABLE IF EXISTS approval_request;
DROP TABLE IF EXISTS outbound_approval;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE approval_request (
    id                    BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    approval_no           VARCHAR(80) NOT NULL,
    business_type         VARCHAR(40) NOT NULL COMMENT 'OUTBOUND/HIGH_RES_DOWNLOAD/REPRODUCTION',
    status                VARCHAR(30) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/COMPLETED',
    applicant_id          BIGINT UNSIGNED NOT NULL,
    approver_id           BIGINT UNSIGNED DEFAULT NULL,
    request_data          JSON DEFAULT NULL,
    request_pdf_object_key VARCHAR(1000) DEFAULT NULL,
    signed_pdf_object_key  VARCHAR(1000) DEFAULT NULL,
    signed_pdf_sha256     CHAR(64) DEFAULT NULL,
    approval_time         DATETIME(3) DEFAULT NULL,
    completed_time        DATETIME(3) DEFAULT NULL,
    remark                VARCHAR(4000) DEFAULT NULL,
    created_at            DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at            DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_approval_no (approval_no),
    KEY idx_approval_business_status (business_type, status, created_at),
    KEY idx_approval_applicant (applicant_id, created_at),
    KEY idx_approval_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='统一审批申请';

CREATE TABLE approval_item (
    id             BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    approval_id    BIGINT UNSIGNED NOT NULL,
    artwork_id     BIGINT UNSIGNED NOT NULL,
    multimedia_id  BIGINT UNSIGNED DEFAULT NULL,
    variant_id     BIGINT UNSIGNED DEFAULT NULL,
    item_data      JSON DEFAULT NULL,
    created_at     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    KEY idx_approval_item_approval (approval_id),
    KEY idx_approval_item_artwork (artwork_id),
    KEY idx_approval_item_multimedia (multimedia_id),
    KEY idx_approval_item_variant (variant_id),
    CONSTRAINT fk_approval_item_request
        FOREIGN KEY (approval_id) REFERENCES approval_request(id),
    CONSTRAINT fk_approval_item_artwork
        FOREIGN KEY (artwork_id) REFERENCES artwork(id),
    CONSTRAINT fk_approval_item_multimedia
        FOREIGN KEY (multimedia_id) REFERENCES multimedia(id),
    CONSTRAINT fk_approval_item_variant
        FOREIGN KEY (variant_id) REFERENCES multimedia_variant(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审批业务明细';

CREATE TABLE approval_history (
    id             BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    approval_id    BIGINT UNSIGNED NOT NULL,
    action         VARCHAR(50) NOT NULL,
    from_status    VARCHAR(30) DEFAULT NULL,
    to_status      VARCHAR(30) DEFAULT NULL,
    operator_id    BIGINT UNSIGNED DEFAULT NULL,
    detail         JSON DEFAULT NULL,
    pdf_object_key VARCHAR(1000) DEFAULT NULL,
    ip_address     VARCHAR(64) DEFAULT NULL,
    created_at     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    KEY idx_approval_history_approval (approval_id, created_at, id),
    KEY idx_approval_history_action (action, created_at),
    CONSTRAINT fk_approval_history_request
        FOREIGN KEY (approval_id) REFERENCES approval_request(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审批过程审计记录，只允许追加';

ALTER TABLE artwork_history
    DROP FOREIGN KEY fk_history_outbound,
    DROP COLUMN related_outbound_id;

ALTER TABLE artwork_history
    ADD COLUMN related_approval_id BIGINT UNSIGNED DEFAULT NULL AFTER detail,
    ADD KEY idx_history_approval (related_approval_id),
    ADD CONSTRAINT fk_history_approval
        FOREIGN KEY (related_approval_id) REFERENCES approval_request(id);

-- Inventory movement, if present in the deployed schema, should reference
-- approval_request instead of the legacy outbound id in a follow-up migration.
