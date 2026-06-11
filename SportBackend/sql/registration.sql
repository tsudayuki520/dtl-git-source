-- 报名记录表
CREATE TABLE IF NOT EXISTS registration (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '报名记录ID',
    participant_id  BIGINT       NOT NULL COMMENT '参赛人员ID（逻辑外键→participant）',
    event_id        BIGINT       NOT NULL COMMENT '项目ID（逻辑外键→event）',
    schedule_id     BIGINT       DEFAULT NULL COMMENT '赛程ID（逻辑外键→schedule）',
    status          TINYINT DEFAULT 0 COMMENT '状态：0-已报名，1-已晋级，2-已取消',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP NULL COMMENT '创建时间',
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报名记录表';
