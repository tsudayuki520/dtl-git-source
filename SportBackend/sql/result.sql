-- 成绩表
CREATE TABLE IF NOT EXISTS result (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '成绩ID',
    sports_meeting_id BIGINT       NOT NULL COMMENT '所属运动会ID',
    event_id          BIGINT       NOT NULL COMMENT '比赛项目ID（关联event表）',
    event_schedule_id BIGINT       DEFAULT NULL COMMENT '项目-赛程关联ID（关联event_schedule.id）',
    participant_id    BIGINT       NOT NULL COMMENT '参赛人员ID（关联participant表）',
    score_value       INT          DEFAULT NULL COMMENT '成绩值（径赛=毫秒数，田赛=厘米数，按event.category区分）',
    points            INT          DEFAULT 0 COMMENT '积分（用于代表队总分计算）',
    record_status     TINYINT      NOT NULL DEFAULT 0 COMMENT '破纪录审核状态（0=未审，1=通过已入册，2=拒绝）',
    create_time       DATETIME DEFAULT CURRENT_TIMESTAMP NULL COMMENT '创建时间',
    update_time       DATETIME DEFAULT CURRENT_TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成绩表';
