-- ============================================================
-- DreiStrom MVP – Compliance Event & Threshold Alert Schema (V8)
-- MySQL 8.4 LTS | InnoDB | utf8mb4_unicode_ci
-- ============================================================

-- -----------------------------------------------------------
-- compliance_event – tax deadlines & regulatory compliance events
-- Tracks due dates for USt-VA, ESt-VA, GewSt-VA, EÜR filing,
-- Jahresabschluss, and other statutory deadlines.
-- Links to tax_period for traceability.
-- -----------------------------------------------------------
CREATE TABLE compliance_event (
    id                  BIGINT              AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT              NOT NULL,
    event_type          ENUM(
                            'UST_VA',               -- Umsatzsteuer-Voranmeldung
                            'EST_VORAUSZAHLUNG',    -- Einkommensteuer-Vorauszahlung
                            'GEWST_VORAUSZAHLUNG',  -- Gewerbesteuer-Vorauszahlung
                            'EUER_FILING',          -- EÜR submission
                            'EST_DECLARATION',      -- Einkommensteuererklärung
                            'GEWST_DECLARATION',    -- Gewerbesteuererklärung
                            'UST_DECLARATION',      -- Umsatzsteuererklärung
                            'ZM_REPORT',            -- Zusammenfassende Meldung
                            'SOCIAL_INSURANCE',     -- Sozialversicherung
                            'CUSTOM'                -- User-defined deadline
                        )                   NOT NULL,
    title               VARCHAR(255)        NOT NULL COMMENT 'Human-readable event title',
    description         TEXT                NULL,
    due_date            DATE                NOT NULL,
    status              ENUM('UPCOMING','DUE','OVERDUE','COMPLETED','CANCELLED')
                                            NOT NULL DEFAULT 'UPCOMING',
    tax_period_id       BIGINT              NULL COMMENT 'Optional link to tax_period',
    reminder_config     JSON                NULL COMMENT 'Reminder settings: {"channels":["EMAIL","PUSH"],"daysBefore":[14,7,3,1]}',
    completed_at        TIMESTAMP(6)        NULL COMMENT 'When the event was marked completed',
    created_at          TIMESTAMP(6)        NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6)        NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    FOREIGN KEY (user_id)       REFERENCES app_user(id),
    FOREIGN KEY (tax_period_id) REFERENCES tax_period(id),
    INDEX idx_compliance_user_due  (user_id, due_date),
    INDEX idx_compliance_status    (status),
    INDEX idx_compliance_type_year (user_id, event_type, due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------
-- threshold_alert – real-time threshold breach notifications
-- Monitors Kleinunternehmer limits, Abfärbung ratios,
-- Gewerbesteuer Freibetrag proximity, and similar thresholds.
-- -----------------------------------------------------------
CREATE TABLE threshold_alert (
    id                  BIGINT              AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT              NOT NULL,
    alert_type          ENUM(
                            'KLEINUNTERNEHMER_CURRENT',     -- §19 UStG current year limit
                            'KLEINUNTERNEHMER_PROJECTED',   -- §19 UStG projected limit
                            'ABFAERBUNG_RATIO',             -- §15 Abs. 3 Nr. 1 EStG
                            'GEWST_FREIBETRAG',             -- Approaching €24,500 Freibetrag
                            'VAT_THRESHOLD',                -- General VAT threshold
                            'RESERVE_SHORTFALL',            -- Tax reserve below target
                            'CUSTOM'                        -- User-defined threshold
                        )                   NOT NULL,
    current_value_cents BIGINT              NOT NULL COMMENT 'Current observed value in cents',
    threshold_value_cents BIGINT            NOT NULL COMMENT 'Threshold value in cents',
    threshold_percent   DECIMAL(5,2)        NULL COMMENT 'Usage as percentage of threshold',
    triggered_at        TIMESTAMP(6)        NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    acknowledged        BOOLEAN             NOT NULL DEFAULT FALSE,
    acknowledged_at     TIMESTAMP(6)        NULL,
    message             VARCHAR(500)        NULL COMMENT 'Human-readable alert message',
    created_at          TIMESTAMP(6)        NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6)        NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    FOREIGN KEY (user_id) REFERENCES app_user(id),
    INDEX idx_alert_user_type   (user_id, alert_type),
    INDEX idx_alert_unack       (user_id, acknowledged, triggered_at),
    INDEX idx_alert_triggered   (triggered_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
