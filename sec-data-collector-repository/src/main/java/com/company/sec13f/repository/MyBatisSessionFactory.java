package com.company.sec13f.repository;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * MyBatis会话工厂管理器
 * 提供SqlSession的创建和管理
 */
public class MyBatisSessionFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(MyBatisSessionFactory.class);
    private static SqlSessionFactory sqlSessionFactory;
    
    static {
        try {
            String resource = "mybatis-config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
            logger.info("MyBatis SqlSessionFactory initialized successfully");
        } catch (IOException e) {
            logger.error("Failed to initialize MyBatis SqlSessionFactory", e);
            throw new RuntimeException("Failed to initialize MyBatis", e);
        }
    }
    
    /**
     * 获取SqlSessionFactory实例
     */
    public static SqlSessionFactory getSqlSessionFactory() {
        return sqlSessionFactory;
    }
    
    /**
     * 获取新的SqlSession（需要手动管理事务）
     */
    public static SqlSession openSession() {
        return sqlSessionFactory.openSession();
    }
    
    /**
     * 获取新的SqlSession（自动提交事务）
     */
    public static SqlSession openSession(boolean autoCommit) {
        return sqlSessionFactory.openSession(autoCommit);
    }
    
    /**
     * 初始化数据库表结构
     */
    public static void initializeTables() {
        try (SqlSession session = openSession(true)) {
            // 创建Filing表
            session.update("DatabaseInit.createFilingTable");
            // 创建Holding表  
            session.update("DatabaseInit.createHoldingTable");
            
            logger.info("Database tables initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize database tables", e);
            throw new RuntimeException("Failed to initialize database tables", e);
        }
    }
}