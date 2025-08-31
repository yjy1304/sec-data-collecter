package com.company.sec13f.service;

import com.company.sec13f.repository.entity.Task;
import com.company.sec13f.repository.enums.TaskStatus;
import com.company.sec13f.repository.enums.TaskType;
import com.company.sec13f.repository.mapper.TaskMapper;
import com.company.sec13f.service.plugin.TaskProcessPlugin;
import com.company.sec13f.service.plugin.TaskResult;
import com.company.sec13f.service.util.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

// import javax.annotation.PostConstruct; // Not available in Java 8, will implement init differently
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 任务服务 - 负责任务的调度执行
 */
@Service
public class TaskService {
    
    private final Map<TaskType, TaskProcessPlugin> pluginMap = new HashMap<>();
    private final TaskMapper taskMapper;
    private final Logger logger;
    private final ExecutorService executorService;
    
    @Autowired
    public TaskService(TaskMapper taskMapper) {
        this.taskMapper = taskMapper;
        this.logger = Logger.getInstance();
        this.executorService = Executors.newFixedThreadPool(3);
    }
    
    // @PostConstruct // Not available in Java 8
    public void init() {
        try {
            // MyBatis会自动处理表结构，不需要手动初始化
            logger.info("✅ TaskService初始化成功");
        } catch (Exception e) {
            logger.error("❌ TaskService初始化失败", e);
        }
    }
    
    /**
     * 注册任务处理插件
     */
    public void registerPlugin(TaskProcessPlugin plugin) {
        pluginMap.put(plugin.getTaskType(), plugin);
    }
    
    /**
     * 执行单个任务
     */
    public CompletableFuture<TaskResult> handleTask(Task task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 标记任务开始执行
                task.setStarted();
                taskMapper.update(task);
                
                // 获取对应的任务处理插件
                TaskProcessPlugin plugin = pluginMap.get(task.getTaskType());
                if (plugin == null) {
                    String errorMsg = "未找到类型为 " + task.getTaskType() + " 的任务处理插件";
                    task.setFailed(errorMsg);
                    taskMapper.update(task);
                    return TaskResult.failure(errorMsg);
                }
                
                logger.info("🚀 开始执行任务: " + task.getTaskId() + " [" + task.getTaskType() + "]");
                
                // 执行任务
                TaskResult result = plugin.handleTask(task);
                
                // 更新任务状态
                if (result.isSuccess()) {
                    task.setCompleted(result.getMessage());
                    logger.info("✅ 任务完成: " + task.getTaskId());
                } else {
                    // 检查是否需要重试
                    if (task.needsRetry(3)) { // 最大重试3次
                        task.setForRetry(result.getMessage());
                        logger.warn("🔄 任务需要重试: " + task.getTaskId() + " (第" + task.getRetryTimes() + "次)");
                    } else {
                        task.setFailed(result.getMessage());
                        logger.error("❌ 任务失败: " + task.getTaskId());
                    }
                }
                taskMapper.update(task);
                
                return result;
                
            } catch (Exception e) {
                task.setFailed("任务执行异常: " + e.getMessage());
                taskMapper.update(task);
                logger.error("💥 任务执行异常: " + task.getTaskId(), e);
                return TaskResult.failure("任务执行异常: " + e.getMessage(), e);
            }
        }, executorService);
    }
    
    /**
     * 定时调度任务
     * 每10分钟执行一次，捞取待执行和重试的任务
     */
    @Scheduled(cron = "0 */10 * * * ?")
    public void scheduleTask() {
        try {
            logger.info("🕐 开始定时任务调度...");
            
            // 获取待执行的任务 (PENDING状态)
            List<Task> pendingTasks = taskMapper.selectPendingTasks();
            
            // 获取需要重试的任务 (RETRY状态且next_execute_time已到期)
            String currentTime = LocalDateTime.now().toString();
            List<Task> retryTasks = taskMapper.selectRetryTasksReadyForExecution(currentTime);
            
            int totalTasks = pendingTasks.size() + retryTasks.size();
            if (totalTasks == 0) {
                logger.info("💤 没有需要执行的任务");
                return;
            }
            
            logger.info("📋 发现 " + totalTasks + " 个待执行任务 (待处理:" + pendingTasks.size() + ", 重试:" + retryTasks.size() + ")");
            
            // 执行待处理任务
            pendingTasks.forEach(this::handleTask);
            
            // 执行重试任务  
            retryTasks.forEach(this::handleTask);
            
            logger.info("✅ 定时任务调度完成，共处理 " + totalTasks + " 个任务");
            
        } catch (Exception e) {
            logger.error("❌ 定时任务调度失败", e);
        }
    }
    
    /**
     * 创建新任务
     */
    public String createTask(TaskType taskType, String taskParameters) {
        try {
            Task task = new Task(java.util.UUID.randomUUID().toString(), taskType);
            task.setTaskParameters(taskParameters);
            
            taskMapper.insert(task);
            logger.info("💾 创建新任务: " + task.getTaskId() + " [" + taskType + "]");
            
            return task.getTaskId();
            
        } catch (Exception e) {
            logger.error("❌ 创建任务失败", e);
            throw new RuntimeException("Failed to create task", e);
        }
    }
    
    /**
     * 获取任务状态
     */
    public Task getTaskStatus(String taskId) {
        return taskMapper.selectByTaskId(taskId);
    }
    
    /**
     * 获取所有任务
     */
    public List<Task> getAllTasks() {
        return taskMapper.selectAll();
    }
}