-- 参赛人员表
CREATE TABLE IF NOT EXISTS participant (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '参赛人员ID',
    sports_meeting_id BIGINT       NOT NULL COMMENT '所属运动会ID',
    team_id           BIGINT       DEFAULT NULL COMMENT '代表队ID（关联team表）',
    user_code         VARCHAR(30)  NOT NULL COMMENT '学号/工号',
    name              VARCHAR(50)  NOT NULL COMMENT '姓名',
    phone             VARCHAR(20)  NOT NULL COMMENT '电话号码',
    gender            VARCHAR(4)   NOT NULL COMMENT '性别',
    college           VARCHAR(100) DEFAULT NULL COMMENT '学院',
    major             VARCHAR(100) DEFAULT NULL COMMENT '专业',
    create_time       DATETIME DEFAULT CURRENT_TIMESTAMP NULL COMMENT '创建时间',
    update_time       DATETIME DEFAULT CURRENT_TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='参赛人员表';
