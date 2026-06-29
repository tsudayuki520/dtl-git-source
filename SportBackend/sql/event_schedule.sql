CREATE TABLE IF NOT EXISTS event_schedule (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '关联ID',
    event_id       BIGINT NOT NULL COMMENT '项目ID',
    schedule_id    BIGINT NOT NULL COMMENT '赛程ID',
    allow_register TINYINT NOT NULL DEFAULT 0 COMMENT '该轮次是否开放报名：1-开放，0-关闭',
    is_deleted     TINYINT DEFAULT 0 COMMENT '软删除：0-正常，1-已删除',
    create_time    DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_event_schedule (event_id, schedule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目-赛程关联表';
