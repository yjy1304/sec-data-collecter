package com.company.sec13f.repository.database;

import com.company.sec13f.repository.MyBatisSessionFactory;
import com.company.sec13f.repository.entity.Task;
import com.company.sec13f.repository.enums.TaskStatus;
import com.company.sec13f.repository.enums.TaskType;
import com.company.sec13f.repository.mapper.TaskMapper;
import com.company.sec13f.repository.util.Logger;
import org.apache.ibatis.session.SqlSession;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 通用任务数据访问对象
 * 基于MyBatis实现
 */
@Repository
public class UniversalTaskDAO {
    
    private final Logger logger = Logger.getInstance();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * 初始化tasks表
     */
    public void initializeTasksTable() {
        String sql = "CREATE TABLE IF NOT EXISTS tasks (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
            "task_id VARCHAR(100) UNIQUE NOT NULL, " +
            "task_type VARCHAR(50) NOT NULL, " +
            "status VARCHAR(50) NOT NULL, " +
            "message TEXT, " +
            "task_parameters TEXT, " +
            "retry_times INT DEFAULT 0, " +
            "start_time TIMESTAMP NULL, " +
            "next_execute_time TIMESTAMP NULL, " +
            "end_time TIMESTAMP NULL, " +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
            "INDEX idx_task_id (task_id), " +
            "INDEX idx_task_type (task_type), " +
            "INDEX idx_status (status)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
            
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            session.getConnection().createStatement().execute(sql);
            session.commit();
            logger.info("✅ 通用tasks表初始化成功");
        } catch (Exception e) {
            logger.error("❌ 通用tasks表初始化失败", e);
            throw new RuntimeException("Failed to initialize tasks table", e);
        }
    }
    
    /**
     * 保存任务
     */
    public void saveTask(Task task) {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            TaskMapper mapper = session.getMapper(TaskMapper.class);
            
            if (mapper.existsByTaskId(task.getTaskId())) {
                int updated = mapper.update(task);
                if (updated > 0) {
                    logger.info("📝 更新任务: " + task.getTaskId());
                } else {
                    logger.warn("⚠️ 更新任务失败: " + task.getTaskId());
                }
            } else {
                int inserted = mapper.insert(task);
                if (inserted > 0) {
                    logger.info("💾 保存新任务: " + task.getTaskId());
                } else {
                    logger.warn("⚠️ 保存任务失败: " + task.getTaskId());
                }
            }
            
            session.commit();
        } catch (Exception e) {
            logger.error("❌ 保存任务失败: " + task.getTaskId(), e);
            throw new RuntimeException("Failed to save task", e);
        }
    }
    
    /**
     * 根据任务ID查询任务
     */
    public Task findByTaskId(String taskId) {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            TaskMapper mapper = session.getMapper(TaskMapper.class);
            return mapper.selectByTaskId(taskId);
        } catch (Exception e) {
            logger.error("❌ 查询任务失败: " + taskId, e);
            throw new RuntimeException("Failed to find task by taskId", e);
        }
    }
    
    /**
     * 根据状态查询任务
     */
    public List<Task> findByStatus(TaskStatus status) {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            TaskMapper mapper = session.getMapper(TaskMapper.class);
            return mapper.selectByStatus(status.name());
        } catch (Exception e) {
            logger.error("❌ 根据状态查询任务失败: " + status, e);
            throw new RuntimeException("Failed to find tasks by status", e);
        }
    }
    
    /**
     * 查询待处理的任务
     */
    public List<Task> findPendingTasks() {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            TaskMapper mapper = session.getMapper(TaskMapper.class);
            return mapper.selectPendingTasks();
        } catch (Exception e) {
            logger.error("❌ 查询待处理任务失败", e);
            throw new RuntimeException("Failed to find pending tasks", e);
        }
    }
    
    /**
     * 查询需要重试的任务
     */
    public List<Task> findRetryTasksReadyForExecution(LocalDateTime currentTime) {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            TaskMapper mapper = session.getMapper(TaskMapper.class);
            String timeStr = currentTime.format(DATE_TIME_FORMATTER);
            return mapper.selectRetryTasksReadyForExecution(timeStr);
        } catch (Exception e) {
            logger.error("❌ 查询重试任务失败", e);
            throw new RuntimeException("Failed to find retry tasks", e);
        }
    }
    
    /**
     * 查询所有任务
     */
    public List<Task> findAllTasks() {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            TaskMapper mapper = session.getMapper(TaskMapper.class);
            return mapper.selectAll();
        } catch (Exception e) {
            logger.error("❌ 查询所有任务失败", e);
            throw new RuntimeException("Failed to find all tasks", e);
        }
    }
    
    /**
     * 更新任务
     */
    public void updateTask(Task task) {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            TaskMapper mapper = session.getMapper(TaskMapper.class);
            int updated = mapper.update(task);
            if (updated > 0) {
                logger.info("📝 更新任务: " + task.getTaskId() + " -> " + task.getStatus());
            } else {
                logger.warn("⚠️ 更新任务失败: " + task.getTaskId());
            }
            session.commit();
        } catch (Exception e) {
            logger.error("❌ 更新任务失败: " + task.getTaskId(), e);
            throw new RuntimeException("Failed to update task", e);
        }
    }
    
    /**
     * 删除任务
     */
    public void deleteTask(String taskId) {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            TaskMapper mapper = session.getMapper(TaskMapper.class);
            int deleted = mapper.deleteByTaskId(taskId);
            if (deleted > 0) {
                logger.info("🗑️ 删除任务: " + taskId);
            } else {
                logger.warn("⚠️ 删除任务失败: " + taskId);
            }
            session.commit();
        } catch (Exception e) {
            logger.error("❌ 删除任务失败: " + taskId, e);
            throw new RuntimeException("Failed to delete task", e);
        }
    }
    
    /**
     * 统计任务数量
     */
    public long countTasks() {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            TaskMapper mapper = session.getMapper(TaskMapper.class);
            return mapper.countAll();
        } catch (Exception e) {
            logger.error("❌ 统计任务数量失败", e);
            throw new RuntimeException("Failed to count tasks", e);
        }
    }
    
    /**
     * 根据状态统计任务数量
     */
    public long countTasksByStatus(TaskStatus status) {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            TaskMapper mapper = session.getMapper(TaskMapper.class);
            return mapper.countByStatus(status.name());
        } catch (Exception e) {
            logger.error("❌ 根据状态统计任务数量失败: " + status, e);
            throw new RuntimeException("Failed to count tasks by status", e);
        }
    }
}