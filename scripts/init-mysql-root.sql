-- 使用root用户创建SEC 13F数据库
CREATE DATABASE IF NOT EXISTS sec13f 
    CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE sec13f;

-- 显示确认信息
SELECT 'MySQL database sec13f created successfully!' as Status;
SELECT 'Using root user for database access' as Info;