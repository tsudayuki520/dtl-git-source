-- 组别表（关联运动会，每个运动会可自定义组别）
CREATE TABLE IF NOT EXISTS group_type (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '组别ID',
    sports_meeting_id BIGINT       NOT NULL COMMENT '所属运动会ID',
    name              VARCHAR(50)  NOT NULL COMMENT '组别名称：学生组、教职工组等',
    per_team_limit         INT          DEFAULT 0 COMMENT '每代表队在选定项目中限报人数(0=不限)',
    limit_event_ids        JSON         DEFAULT NULL COMMENT '每代表队限报选中的项目ID数组(空/NULL=不限)',
    per_person_limit       INT          DEFAULT 0 COMMENT '每人限报项目数(0=不限)',
    person_limit_event_ids JSON         DEFAULT NULL COMMENT '每人限报选中的项目ID数组(空/NULL=不限)',
    create_time            DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组别表';
