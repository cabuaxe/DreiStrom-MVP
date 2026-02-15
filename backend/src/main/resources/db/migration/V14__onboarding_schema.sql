-- Onboarding schema: registration steps and decision points
-- 15-step master checklist from Tax Guide §9

CREATE TABLE registration_step (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    step_number     INT             NOT NULL,
    title           VARCHAR(255)    NOT NULL,
    description     TEXT,
    status          ENUM('NOT_STARTED','IN_PROGRESS','COMPLETED','BLOCKED')
                    NOT NULL DEFAULT 'NOT_STARTED',
    responsible     ENUM('USER','SYSTEM','STEUERBERATER') NOT NULL DEFAULT 'USER',
    dependencies    JSON,
    completed_at    TIMESTAMP(6),
    created_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_step_user FOREIGN KEY (user_id)
        REFERENCES app_user(id) ON DELETE RESTRICT,

    UNIQUE KEY uk_step_user_number (user_id, step_number),
    INDEX idx_step_user_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE decision_point (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    step_id         BIGINT          NOT NULL,
    question        VARCHAR(500)    NOT NULL,
    option_a        VARCHAR(500)    NOT NULL,
    option_b        VARCHAR(500)    NOT NULL,
    recommendation  ENUM('OPTION_A','OPTION_B') NOT NULL,
    recommendation_reason TEXT,
    user_choice     ENUM('OPTION_A','OPTION_B'),
    decided_at      TIMESTAMP(6),
    created_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_decision_step FOREIGN KEY (step_id)
        REFERENCES registration_step(id) ON DELETE CASCADE,

    INDEX idx_decision_step (step_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
