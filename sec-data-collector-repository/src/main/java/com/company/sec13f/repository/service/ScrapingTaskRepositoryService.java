package com.company.sec13f.repository.service;

import com.company.sec13f.repository.MyBatisSessionFactory;
import com.company.sec13f.repository.entity.ScrapingTask;
import com.company.sec13f.repository.mapper.ScrapingTaskMapper;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * ScrapingTask数据仓储服务
 * 提供任务管理相关的数据访问操作
 */
public class ScrapingTaskRepositoryService {
    
    private static final Logger logger = LoggerFactory.getLogger(ScrapingTaskRepositoryService.class);
    
    /**
     * 保存新任务
     */
    public Long saveTask(ScrapingTask task) {
        try (SqlSession session = MyBatisSessionFactory.openSession(true)) {
            ScrapingTaskMapper mapper = session.getMapper(ScrapingTaskMapper.class);
            mapper.insert(task);
            logger.info("📝 Task saved: {} - {}", task.getTaskId(), task.getCompanyName());
            return task.getId();
        } catch (Exception e) {
            logger.error("❌ Failed to save task: {}", task.getTaskId(), e);
            throw new RuntimeException("Failed to save task: " + e.getMessage(), e);
        }
    }
    
    /**
     * 更新任务状态
     */
    public void updateTask(ScrapingTask task) {
        try (SqlSession session = MyBatisSessionFactory.openSession(true)) {
            ScrapingTaskMapper mapper = session.getMapper(ScrapingTaskMapper.class);
            int updated = mapper.update(task);
            
            if (updated > 0) {
                logger.debug("🔄 Task updated: {} - Status: {}", task.getTaskId(), task.getStatus());
            } else {
                logger.warn("⚠️ No task updated for ID: {}", task.getTaskId());
            }
        } catch (Exception e) {
            logger.error("❌ Failed to update task: {}", task.getTaskId(), e);
            throw new RuntimeException("Failed to update task: " + e.getMessage(), e);
        }
    }
    
    /**
     * 保存或更新任务
     */
    public void saveOrUpdateTask(ScrapingTask task) {
        try (SqlSession session = MyBatisSessionFactory.openSession(true)) {
            ScrapingTaskMapper mapper = session.getMapper(ScrapingTaskMapper.class);
            
            if (mapper.existsByTaskId(task.getTaskId())) {
                mapper.update(task);
                logger.debug("🔄 Task updated: {}", task.getTaskId());
            } else {
                mapper.insert(task);
                logger.info("📝 New task saved: {}", task.getTaskId());
            }
        } catch (Exception e) {
            logger.error("❌ Failed to save or update task: {}", task.getTaskId(), e);
            throw new RuntimeException("Failed to save or update task: " + e.getMessage(), e);
        }
    }
    
    /**
     * 根据任务ID查询任务
     */
    public ScrapingTask getTaskByTaskId(String taskId) {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            ScrapingTaskMapper mapper = session.getMapper(ScrapingTaskMapper.class);
            return mapper.selectByTaskId(taskId);
        }
    }
    
    /**
     * 获取所有任务
     */
    public List<ScrapingTask> getAllTasks() {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            ScrapingTaskMapper mapper = session.getMapper(ScrapingTaskMapper.class);
            return mapper.selectAll();
        }
    }
    
    /**
     * 根据状态查询任务
     */
    public List<ScrapingTask> getTasksByStatus(ScrapingTask.TaskStatus status) {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            ScrapingTaskMapper mapper = session.getMapper(ScrapingTaskMapper.class);
            return mapper.selectByStatus(status);
        }
    }
    
    /**
     * 获取失败的任务（用于重试）
     */
    public List<ScrapingTask> getFailedTasks() {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            ScrapingTaskMapper mapper = session.getMapper(ScrapingTaskMapper.class);
            return mapper.selectFailedTasks();
        }
    }
    
    /**
     * 获取运行中的任务
     */
    public List<ScrapingTask> getRunningTasks() {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            ScrapingTaskMapper mapper = session.getMapper(ScrapingTaskMapper.class);
            return mapper.selectRunningTasks();
        }
    }
    
    /**
     * 获取最近的N个任务
     */
    public List<ScrapingTask> getRecentTasks(int limit) {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            ScrapingTaskMapper mapper = session.getMapper(ScrapingTaskMapper.class);
            return mapper.selectRecentTasks(limit);
        }
    }
    
    /**
     * 根据CIK查询任务
     */
    public List<ScrapingTask> getTasksByCik(String cik) {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            ScrapingTaskMapper mapper = session.getMapper(ScrapingTaskMapper.class);
            return mapper.selectByCik(cik);
        }
    }
    
    /**
     * 删除任务
     */
    public boolean deleteTask(String taskId) {
        try (SqlSession session = MyBatisSessionFactory.openSession(true)) {
            ScrapingTaskMapper mapper = session.getMapper(ScrapingTaskMapper.class);
            int deleted = mapper.deleteByTaskId(taskId);
            
            if (deleted > 0) {
                logger.info("🗑️ Task deleted: {}", taskId);
                return true;
            } else {
                logger.warn("⚠️ No task found to delete: {}", taskId);
                return false;
            }
        } catch (Exception e) {
            logger.error("❌ Failed to delete task: {}", taskId, e);
            throw new RuntimeException("Failed to delete task: " + e.getMessage(), e);
        }
    }
    
    /**
     * 清理已完成的任务
     */
    public int cleanupCompletedTasks() {
        try (SqlSession session = MyBatisSessionFactory.openSession(true)) {
            ScrapingTaskMapper mapper = session.getMapper(ScrapingTaskMapper.class);
            int deleted = mapper.deleteCompletedTasks();
            
            if (deleted > 0) {
                logger.info("🧹 Cleaned up {} completed tasks", deleted);
            }
            return deleted;
        } catch (Exception e) {
            logger.error("❌ Failed to cleanup completed tasks", e);
            throw new RuntimeException("Failed to cleanup completed tasks: " + e.getMessage(), e);
        }
    }
    
    /**
     * 清理指定天数前的旧任务
     */
    public int cleanupOldTasks(int days) {
        try (SqlSession session = MyBatisSessionFactory.openSession(true)) {
            ScrapingTaskMapper mapper = session.getMapper(ScrapingTaskMapper.class);
            int deleted = mapper.deleteTasksOlderThan(days);
            
            if (deleted > 0) {
                logger.info("🧹 Cleaned up {} tasks older than {} days", deleted, days);
            }
            return deleted;
        } catch (Exception e) {
            logger.error("❌ Failed to cleanup old tasks", e);
            throw new RuntimeException("Failed to cleanup old tasks: " + e.getMessage(), e);
        }
    }
    
    /**
     * 统计任务总数
     */
    public long countTasks() {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            ScrapingTaskMapper mapper = session.getMapper(ScrapingTaskMapper.class);
            return mapper.countAll();
        }
    }
    
    /**
     * 根据状态统计任务数量
     */
    public long countTasksByStatus(ScrapingTask.TaskStatus status) {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            ScrapingTaskMapper mapper = session.getMapper(ScrapingTaskMapper.class);
            return mapper.countByStatus(status);
        }
    }
    
    /**
     * 统计今日任务数量
     */
    public long countTodayTasks() {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            ScrapingTaskMapper mapper = session.getMapper(ScrapingTaskMapper.class);
            return mapper.countTodayTasks();
        }
    }
    
    /**
     * 检查任务是否存在
     */
    public boolean taskExists(String taskId) {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            ScrapingTaskMapper mapper = session.getMapper(ScrapingTaskMapper.class);
            return mapper.existsByTaskId(taskId);
        }
    }
}