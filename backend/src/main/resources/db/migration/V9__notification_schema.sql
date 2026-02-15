-- ============================================================
-- DreiStrom MVP – Notification Schema (V9)
-- MySQL 8.4 LTS | InnoDB | utf8mb4_unicode_ci
-- ============================================================

-- -----------------------------------------------------------
-- notification – in-app notifications and delivery tracking
-- Supports multi-channel delivery (EMAIL, IN_APP, PUSH).
-- -----------------------------------------------------------
CREATE TABLE notification (
    id                  BIGINT              AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT              NOT NULL,
    compliance_event_id BIGINT              NULL COMMENT 'Source compliance event',
    channel             ENUM('EMAIL','IN_APP','PUSH')
                                            NOT NULL,
    title               VARCHAR(255)        NOT NULL,
    message             TEXT                NOT NULL,
    days_before         INT                 NOT NULL COMMENT 'Reminder interval (14, 7, 3, 1)',
    delivered           BOOLEAN             NOT NULL DEFAULT FALSE,
    read_at             TIMESTAMP(6)        NULL,
    created_at          TIMESTAMP(6)        NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    FOREIGN KEY (user_id) REFERENCES app_user(id),
    FOREIGN KEY (compliance_event_id) REFERENCES compliance_event(id),
    INDEX idx_notification_user     (user_id, delivered, created_at),
    INDEX idx_notification_event    (compliance_event_id),
    INDEX idx_notification_channel  (channel, delivered)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
