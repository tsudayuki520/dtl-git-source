-- 参赛人员表（某运动会的参赛身份）
CREATE TABLE IF NOT EXISTS participant (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '参赛人员ID',
    sports_meeting_id BIGINT NOT NULL COMMENT '所属运动会ID',
    user_id           BIGINT NOT NULL COMMENT '关联用户ID',
    team_id           BIGINT DEFAULT NULL COMMENT '代表队ID（关联team表）',
    create_time       DATETIME DEFAULT CURRENT_TIMESTAMP NULL COMMENT '创建时间',
    update_time       DATETIME DEFAULT CURRENT_TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_meeting_user (sports_meeting_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='参赛人员表（运动会参赛身份）';
