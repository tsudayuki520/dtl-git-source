-- 运动会记录表
CREATE TABLE IF NOT EXISTS record (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    group_type        VARCHAR(50)  DEFAULT NULL COMMENT '组别',
    event_name        VARCHAR(100) DEFAULT NULL COMMENT '项目',
    unit              VARCHAR(100) DEFAULT NULL COMMENT '单位（代表队）',
    name              VARCHAR(50)  DEFAULT NULL COMMENT '姓名',
    score             DECIMAL(10,2) DEFAULT NULL COMMENT '成绩',
    record_time       DATETIME     DEFAULT NULL COMMENT '时间',
    venue             VARCHAR(200) DEFAULT NULL COMMENT '地点',
    create_time       DATETIME DEFAULT CURRENT_TIMESTAMP NULL COMMENT '创建时间',
    update_time       DATETIME DEFAULT CURRENT_TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运动会记录表';
