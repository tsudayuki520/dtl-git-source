CREATE TABLE IF NOT EXISTS team_score_adjustment (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    team_id       BIGINT NOT NULL COMMENT '所属代表队',
    delta_amount  DECIMAL(8,2) NOT NULL COMMENT '调整数额（正=加分，负=扣分）',
    note          VARCHAR(255) NOT NULL COMMENT '调整原因，如：xxx作弊扣5分',
    create_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_team_id (team_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代表队总分调整记录';
