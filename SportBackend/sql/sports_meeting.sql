-- 运动会表
CREATE TABLE IF NOT EXISTS sports_meeting (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '运动会ID',
    name VARCHAR(100) NOT NULL COMMENT '运动会名称',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-报名中，1-进行中，2-已结束',
    organizer VARCHAR(100) NOT NULL COMMENT '主办单位',
    contact_phone VARCHAR(20) NOT NULL COMMENT '联系电话',
    venue VARCHAR(200) NOT NULL COMMENT '比赛地址',
    registration_start DATETIME NOT NULL COMMENT '报名开始时间',
    registration_end DATETIME NOT NULL COMMENT '报名截止时间',
    competition_date DATE NOT NULL COMMENT '比赛日期',
    schedule_id BIGINT DEFAULT NULL COMMENT '赛程ID（逻辑外键）',
    sign_sheet_id BIGINT DEFAULT NULL COMMENT '签名表ID（逻辑外键）',
    player_list_id BIGINT DEFAULT NULL COMMENT '选手名单ID（逻辑外键）',
    event_id BIGINT DEFAULT NULL COMMENT '比赛项目ID（逻辑外键）',
    result_id BIGINT DEFAULT NULL COMMENT '成绩公告ID（逻辑外键）',
    notice_id BIGINT DEFAULT NULL COMMENT '赛事通知ID（逻辑外键）',
    appeal_id BIGINT DEFAULT NULL COMMENT '申述表ID（逻辑外键）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运动会表';
