-- 创建SEC 13F数据库
CREATE DATABASE IF NOT EXISTS sec13f 
    CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci;

-- 创建用户（如果不存在）
CREATE USER IF NOT EXISTS 'sec_user'@'localhost' IDENTIFIED BY 'sec_password';

-- 授予权限
GRANT ALL PRIVILEGES ON sec13f.* TO 'sec_user'@'localhost';
GRANT CREATE, DROP ON sec13f.* TO 'sec_user'@'localhost';

-- 刷新权限
FLUSH PRIVILEGES;

-- 使用数据库
USE sec13f;

-- 显示确认信息
SELECT 'MySQL database sec13f created successfully!' as Status;
SELECT 'User sec_user created and granted privileges!' as Status;