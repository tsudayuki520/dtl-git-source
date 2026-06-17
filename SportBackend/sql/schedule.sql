-- 赛程表（轮次：预选赛、复赛、决赛）
CREATE TABLE IF NOT EXISTS schedule (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '赛程ID',
    sports_meeting_id BIGINT                             NOT NULL COMMENT '所属运动会ID',
    name              VARCHAR(100)                       NOT NULL COMMENT '轮次名称：预选赛、复赛、决赛',
    sort              INT       DEFAULT 0                 NULL COMMENT '轮次序号：越小越靠前（晋级方向由小到大）',
    status            TINYINT  DEFAULT 0                 NULL COMMENT '状态：0-进行中，1-已结束',
    is_deleted        TINYINT  DEFAULT 0                 NULL COMMENT '软删除：0-正常，1-已删除',
    create_time       DATETIME DEFAULT CURRENT_TIMESTAMP NULL COMMENT '创建时间',
    update_time       DATETIME DEFAULT CURRENT_TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='赛程表';
