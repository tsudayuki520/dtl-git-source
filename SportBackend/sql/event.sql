-- 比赛项目表（具体项目：100米短跑、跳远等）
CREATE TABLE IF NOT EXISTS event (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '项目ID',
    sports_meeting_id BIGINT                             NOT NULL COMMENT '所属运动会ID',
    schedule_id       BIGINT                             NOT NULL COMMENT '所属赛程ID',
    name              VARCHAR(100)                       NOT NULL COMMENT '项目名称：100米短跑、跳远等',
    category          VARCHAR(20)                        NOT NULL DEFAULT '径赛' COMMENT '项目分类：田赛、径赛、趣味赛',
    gender            VARCHAR(10)                        NOT NULL DEFAULT '不限' COMMENT '性别限制：男、女、不限',
    group_type        VARCHAR(20)                        NOT NULL DEFAULT '学生组' COMMENT '组别：学生组、教工组',
    status            TINYINT  DEFAULT 0                 NULL COMMENT '状态：0-进行中，1-已结束',
    is_deleted        TINYINT  DEFAULT 0                 NULL COMMENT '软删除：0-正常，1-已删除',
    create_time       DATETIME DEFAULT CURRENT_TIMESTAMP NULL COMMENT '创建时间',
    update_time       DATETIME DEFAULT CURRENT_TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='比赛项目表';
