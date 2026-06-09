-- 成绩表
CREATE TABLE IF NOT EXISTS result (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '成绩ID',
    sports_meeting_id BIGINT       NOT NULL COMMENT '所属运动会ID',
    event_id          BIGINT       NOT NULL COMMENT '比赛项目ID（关联event表）',
    participant_id    BIGINT       NOT NULL COMMENT '参赛人员ID（关联participant表）',
    score             DECIMAL(10,2) DEFAULT NULL COMMENT '成绩（小数类型，如秒数、米数）',
    create_time       DATETIME DEFAULT CURRENT_TIMESTAMP NULL COMMENT '创建时间',
    update_time       DATETIME DEFAULT CURRENT_TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成绩表';
