package com.company.sec13f.repository.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 测试MySQL数据库连接
 */
public class MySQLConnectionTest {
    
    private static final String DB_URL = "jdbc:mysql://localhost:3306/sec13f?useSSL=false&serverTimezone=UTC&characterEncoding=utf8";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "123456";
    
    public static void main(String[] args) {
        System.out.println("🔍 开始测试MySQL数据库连接...");
        
        try {
            // 加载MySQL驱动
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("✅ MySQL驱动加载成功");
            
            // 测试数据库连接
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                System.out.println("✅ MySQL数据库连接成功！");
                
                // 测试基本SQL查询
                try (Statement stmt = conn.createStatement()) {
                    ResultSet rs = stmt.executeQuery("SELECT VERSION() as version, NOW() as currentTime");
                    if (rs.next()) {
                        System.out.println("📊 MySQL版本: " + rs.getString("version"));
                        System.out.println("🕐 当前时间: " + rs.getString("currentTime"));
                    }
                }
                
                // 测试创建表
                try (Statement stmt = conn.createStatement()) {
                    String testTable = "CREATE TABLE IF NOT EXISTS test_connection (" +
                        "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                        "name VARCHAR(100), " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                        ") ENGINE=InnoDB";
                    stmt.execute(testTable);
                    System.out.println("✅ 测试表创建成功");
                    
                    // 插入测试数据
                    stmt.execute("INSERT INTO test_connection (name) VALUES ('MySQL连接测试')");
                    System.out.println("✅ 测试数据插入成功");
                    
                    // 查询测试数据
                    ResultSet rs = stmt.executeQuery("SELECT * FROM test_connection ORDER BY id DESC LIMIT 1");
                    if (rs.next()) {
                        System.out.println("✅ 测试数据查询成功: " + rs.getString("name") + 
                            " (创建时间: " + rs.getString("created_at") + ")");
                    }
                    
                    // 清理测试表
                    stmt.execute("DROP TABLE test_connection");
                    System.out.println("✅ 测试表清理完成");
                }
                
                System.out.println("🎉 MySQL数据库连接测试完成！所有功能正常");
                
            }
        } catch (Exception e) {
            System.err.println("❌ MySQL连接测试失败: " + e.getMessage());
            e.printStackTrace();
            
            System.err.println("\n🔧 解决方案:");
            System.err.println("1. 确保MySQL服务正在运行");
            System.err.println("2. 执行以下SQL创建数据库和用户:");
            System.err.println("   mysql -u root -p < scripts/init-mysql.sql");
            System.err.println("3. 检查MySQL连接参数是否正确");
        }
    }
}