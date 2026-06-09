-- 代表队表
CREATE TABLE IF NOT EXISTS team (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '代表队ID',
    sports_meeting_id BIGINT       NOT NULL COMMENT '所属运动会ID',
    group_type_id     BIGINT       DEFAULT NULL COMMENT '组别ID（关联group_type表）',
    name              VARCHAR(100) NOT NULL COMMENT '代表队名称',
    leader            VARCHAR(50)  DEFAULT NULL COMMENT '领队人',
    coach             VARCHAR(50)  DEFAULT NULL COMMENT '教练员',
    total_score       DECIMAL(8,2) DEFAULT 0 COMMENT '代表队总分',
    create_time       DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代表队表';
