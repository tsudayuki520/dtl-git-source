-- 用户表（账号主体）
CREATE TABLE IF NOT EXISTS user (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    user_code    VARCHAR(30)  NOT NULL COMMENT '学号/工号，登录账号',
    password     VARCHAR(100) NOT NULL COMMENT 'BCrypt 哈希',
    name         VARCHAR(50)  NOT NULL COMMENT '姓名',
    gender       VARCHAR(4)   NOT NULL COMMENT '性别',
    phone        VARCHAR(20)  DEFAULT NULL COMMENT '电话（非必须）',
    college      VARCHAR(100) DEFAULT NULL COMMENT '学院',
    major        VARCHAR(100) DEFAULT NULL COMMENT '专业/单位',
    create_time  DATETIME DEFAULT CURRENT_TIMESTAMP NULL COMMENT '创建时间',
    update_time  DATETIME DEFAULT CURRENT_TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_code (user_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表（账号主体）';
