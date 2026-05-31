-- 创建数据库
CREATE DATABASE IF NOT EXISTS sport_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE sport_db;

-- 轮播图表
CREATE TABLE IF NOT EXISTS banner (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '轮播图ID',
    image_url VARCHAR(500) NOT NULL COMMENT '图片URL（华为云OBS地址）',
    title VARCHAR(100) DEFAULT NULL COMMENT '轮播图标题',
    sort_order INT DEFAULT 0 COMMENT '排序序号，越小越靠前',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='轮播公告表';

-- 插入测试数据（替换为你的实际OBS图片URL）
INSERT INTO banner (image_url, title, sort_order, status) VALUES
('https://your-bucket.obs.cn-north-4.myhuaweicloud.com/banner/test1.jpg', '测试轮播图1', 1, 1),
('https://your-bucket.obs.cn-north-4.myhuaweicloud.com/banner/test2.jpg', '测试轮播图2', 2, 1),
('https://your-bucket.obs.cn-north-4.myhuaweicloud.com/banner/test3.jpg', '测试轮播图3', 3, 1);
