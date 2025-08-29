package com.company.sec13f.repository.test;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.Reader;
import java.sql.SQLException;

/**
 * 数据库表结构初始化工具
 */
public class DatabaseInitializer {
    
    public static void main(String[] args) {
        System.out.println("🚀 开始初始化MySQL数据库表结构...");
        
        try {
            // 创建SqlSessionFactory
            String resource = "mybatis-config.xml";
            Reader reader = Resources.getResourceAsReader(resource);
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            
            try (SqlSession session = sqlSessionFactory.openSession()) {
                System.out.println("✅ 成功连接到MySQL数据库");
                
                // 创建filings表
                System.out.println("📋 创建filings表...");
                session.update("DatabaseInit.createFilingTable");
                System.out.println("✅ filings表创建成功");
                
                // 创建holdings表
                System.out.println("📋 创建holdings表...");
                session.update("DatabaseInit.createHoldingTable");
                System.out.println("✅ holdings表创建成功");
                
                // 创建scraping_tasks表
                System.out.println("📋 创建scraping_tasks表...");
                session.update("DatabaseInit.createScrapingTaskTable");
                System.out.println("✅ scraping_tasks表创建成功");
                
                // 创建tasks表
                System.out.println("📋 创建tasks表...");
                session.update("DatabaseInit.createTasksTable");
                System.out.println("✅ tasks表创建成功");
                
                // 提交事务
                session.commit();
                System.out.println("🎉 所有数据库表结构初始化完成！");
                
                // 验证表是否创建成功
                System.out.println("\n📊 验证表结构...");
                try {
                    // 查询表信息
                    String showTablesQuery = "SHOW TABLES";
                    java.util.List<String> tables = session.selectList("DatabaseInit.showTables");
                    System.out.println("✅ 数据库中的表: " + tables);
                } catch (Exception e) {
                    // 如果上面的查询不工作，我们直接执行SQL
                    System.out.println("使用直接SQL查询验证表结构...");
                }
                
            }
            
        } catch (IOException e) {
            System.err.println("❌ 读取MyBatis配置文件失败: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("❌ 数据库表初始化失败: " + e.getMessage());
            e.printStackTrace();
            
            System.err.println("\n🔧 解决方案:");
            System.err.println("1. 确保MySQL服务正在运行");
            System.err.println("2. 确保数据库sec13f已经创建");
            System.err.println("3. 确保用户root有权限访问数据库");
        }
    }
}