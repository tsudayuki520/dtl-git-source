-- 运动会记录表（历届档案）
CREATE TABLE IF NOT EXISTS record (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    sports_meeting_id BIGINT       DEFAULT NULL COMMENT '所属运动会ID',
    group_type        VARCHAR(50)  DEFAULT NULL COMMENT '组别',
    event_name        VARCHAR(100) DEFAULT NULL COMMENT '项目',
    category          VARCHAR(20)  DEFAULT NULL COMMENT '项目类别（径赛/田赛/团队赛）',
    unit              VARCHAR(100) DEFAULT NULL COMMENT '单位（代表队）',
    name              VARCHAR(50)  DEFAULT NULL COMMENT '姓名',
    score             DECIMAL(10,2) DEFAULT NULL COMMENT '成绩（人类可读：径赛秒，田赛米）',
    score_value       INT          DEFAULT NULL COMMENT '成绩值（径赛=毫秒，田赛=厘米，对齐result.score_value）',
    result_id         BIGINT       DEFAULT NULL COMMENT '源成绩ID（审核通过时追溯）',
    record_time       DATETIME     DEFAULT NULL COMMENT '时间',
    create_time       DATETIME DEFAULT CURRENT_TIMESTAMP NULL COMMENT '创建时间',
    update_time       DATETIME DEFAULT CURRENT_TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运动会记录表（历届档案）';
