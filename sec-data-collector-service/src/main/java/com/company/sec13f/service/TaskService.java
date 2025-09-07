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

import org.springframework.beans.factory.InitializingBean;

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
public class TaskService implements InitializingBean {
    
    private final Map<TaskType, TaskProcessPlugin> pluginMap = new HashMap<>();
    private final TaskMapper taskMapper;
    private final Logger logger;
    private final ExecutorService executorService;
    private final List<TaskProcessPlugin> plugins;
    
    @Autowired
    public TaskService(TaskMapper taskMapper, List<TaskProcessPlugin> plugins) {
        this.taskMapper = taskMapper;
        this.plugins = plugins;
        this.logger = Logger.getInstance();
        this.executorService = Executors.newFixedThreadPool(3);
        
        // 注册插件信息日志
        logger.info("🔧 TaskService构造函数调用 - 注入的插件数量: " + (plugins != null ? plugins.size() : "null"));
        if (plugins != null && !plugins.isEmpty()) {
            for (TaskProcessPlugin plugin : plugins) {
                logger.info("🔍 发现插件: " + plugin.getClass().getSimpleName() + " [TaskType: " + plugin.getTaskType() + "]");
            }
        } else {
            logger.warn("⚠️ TaskService构造函数中plugins列表为空，可能存在依赖注入问题");
        }
    }
    
    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            logger.info("🔧 TaskService初始化开始...");
            
            // 检查插件列表状态
            if (plugins == null) {
                logger.error("❌ TaskService初始化失败：plugins列表为null");
                throw new IllegalStateException("TaskProcessPlugin列表未正确注入");
            }
            
            logger.info("🔍 TaskService发现 " + plugins.size() + " 个插件候选");
            
            // 自动注册所有TaskProcessPlugin
            int registeredCount = 0;
            for (TaskProcessPlugin plugin : plugins) {
                if (plugin != null) {
                    TaskType taskType = plugin.getTaskType();
                    if (taskType != null) {
                        // 检查是否有重复的TaskType
                        if (pluginMap.containsKey(taskType)) {
                            logger.warn("⚠️ 发现重复的TaskType: " + taskType + 
                                      "，已有插件: " + pluginMap.get(taskType).getClass().getSimpleName() +
                                      "，新插件: " + plugin.getClass().getSimpleName());
                        }
                        
                        pluginMap.put(taskType, plugin);
                        registeredCount++;
                        logger.info("📌 注册任务处理插件: " + taskType + " -> " + plugin.getClass().getSimpleName());
                    } else {
                        logger.warn("⚠️ 插件 " + plugin.getClass().getSimpleName() + " 返回null TaskType，跳过注册");
                    }
                } else {
                    logger.warn("⚠️ 发现null插件实例，跳过注册");
                }
            }
            
            if (registeredCount > 0) {
                logger.info("✅ TaskService初始化成功，注册了 " + registeredCount + " 个插件");
                // 打印所有已注册的插件
                for (Map.Entry<TaskType, TaskProcessPlugin> entry : pluginMap.entrySet()) {
                    logger.info("  ➤ " + entry.getKey() + " -> " + entry.getValue().getClass().getSimpleName());
                }
            } else {
                logger.error("❌ TaskService初始化警告：没有成功注册任何插件！");
            }
        } catch (Exception e) {
            logger.error("❌ TaskService初始化失败", e);
            throw e;
        }
    }
    
    /**
     * 注册任务处理插件
     */
    public void registerPlugin(TaskProcessPlugin plugin) {
        if (plugin != null && plugin.getTaskType() != null) {
            pluginMap.put(plugin.getTaskType(), plugin);
            logger.info("📌 手动注册插件: " + plugin.getTaskType() + " -> " + plugin.getClass().getSimpleName());
        }
    }
    
    /**
     * 获取已注册的插件列表
     */
    public Map<TaskType, String> getRegisteredPlugins() {
        Map<TaskType, String> result = new HashMap<>();
        for (Map.Entry<TaskType, TaskProcessPlugin> entry : pluginMap.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getClass().getSimpleName());
        }
        return result;
    }
    
    /**
     * 检查特定插件是否已注册
     */
    public boolean isPluginRegistered(TaskType taskType) {
        return pluginMap.containsKey(taskType);
    }
    
    /**
     * 获取插件注册状态信息
     */
    public String getPluginStatus() {
        StringBuilder status = new StringBuilder();
        status.append("TaskService插件状态:\n");
        status.append("  - 总数: ").append(pluginMap.size()).append("\n");
        for (Map.Entry<TaskType, TaskProcessPlugin> entry : pluginMap.entrySet()) {
            status.append("  - ").append(entry.getKey()).append(": ")
                  .append(entry.getValue().getClass().getSimpleName()).append("\n");
        }
        return status.toString();
    }
    
    /**
     * 执行单个任务
     */
    public CompletableFuture<TaskResult> handleTask(Task task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("🎯 开始处理任务: " + task.getTaskId() + " [" + task.getTaskType() + "] 状态: " + task.getStatus());
                
                // 更新任务开始时间和状态（如果还是PENDING）
                if (task.getStartTime() == null) {
                    task.setStartTime(LocalDateTime.now());
                    logger.debug("⏰ 设置任务开始时间: " + task.getTaskId());
                }
                
                // 获取对应的任务处理插件
                TaskProcessPlugin plugin = pluginMap.get(task.getTaskType());
                if (plugin == null) {
                    String errorMsg = "未找到类型为 " + task.getTaskType() + " 的任务处理插件";
                    logger.error("❌ " + errorMsg);
                    logger.error("🔍 当前已注册的插件: " + pluginMap.keySet());
                    task.setFailed(errorMsg);
                    taskMapper.update(task);
                    return TaskResult.failure(errorMsg);
                }
                
                logger.info("🚀 开始执行任务: " + task.getTaskId() + " [" + task.getTaskType() + "] 使用插件: " + plugin.getClass().getSimpleName());
                
                // 执行任务
                logger.debug("🔧 调用插件处理任务: " + plugin.getClass().getSimpleName() + ".handleTask()");
                TaskResult result = plugin.handleTask(task);
                logger.debug("📊 插件返回结果: success=" + result.isSuccess() + ", message=" + result.getMessage());
                
                // 更新任务状态
                if (result.isSuccess()) {
                    task.setCompleted(result.getMessage());
                    logger.info("✅ 任务完成: " + task.getTaskId() + " - " + result.getMessage());
                } else {
                    // 检查是否需要重试
                    if (task.needsRetry(3)) { // 最大重试3次
                        task.setForRetry(result.getMessage());
                        logger.warn("🔄 任务需要重试: " + task.getTaskId() + " (第" + task.getRetryTimes() + "次) - " + result.getMessage());
                    } else {
                        task.setFailed(result.getMessage());
                        logger.error("❌ 任务最终失败: " + task.getTaskId() + " - " + result.getMessage());
                    }
                }
                
                logger.debug("💾 更新任务状态到数据库: " + task.getTaskId() + " -> " + task.getStatus());
                taskMapper.update(task);
                
                return result;
                
            } catch (Exception e) {
                String errorMsg = "任务执行异常: " + e.getMessage();
                logger.error("💥 任务执行异常: " + task.getTaskId() + " - " + errorMsg, e);
                task.setFailed(errorMsg);
                taskMapper.update(task);
                return TaskResult.failure(errorMsg, e);
            }
        }, executorService);
    }
    
    /**
     * 定时调度任务
     * 每分钟执行一次，捞取待执行和重试的任务
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void scheduleTask() {
        try {
            logger.info("🕐 开始定时任务调度...");
            logger.debug("🔍 当前插件注册状态: " + pluginMap.size() + " 个插件已注册");
            
            // 检查插件是否已注册
            if (pluginMap.isEmpty()) {
                logger.warn("⚠️  没有注册任何任务处理插件！任务无法执行");
                return;
            }
            
            // 获取待执行的任务 (PENDING状态)
            List<Task> pendingTasks = taskMapper.selectPendingTasks();
            logger.debug("🔍 查询到 " + pendingTasks.size() + " 个PENDING任务");
            
            // 获取需要重试的任务 (RETRY状态且next_execute_time已到期)
            String currentTime = LocalDateTime.now().toString();
            List<Task> retryTasks = taskMapper.selectRetryTasksReadyForExecution(currentTime);
            logger.debug("🔍 查询到 " + retryTasks.size() + " 个RETRY任务（时间已到期）");
            
            int totalTasks = pendingTasks.size() + retryTasks.size();
            if (totalTasks == 0) {
                logger.debug("💤 没有需要执行的任务，调度器空闲");
                return;
            }
            
            logger.info("📋 发现 " + totalTasks + " 个待执行任务 (待处理:" + pendingTasks.size() + ", 重试:" + retryTasks.size() + ")");
            
            // 执行待处理任务
            logger.debug("🚀 开始处理PENDING任务...");
            pendingTasks.forEach(task -> {
                logger.info("📝 调度PENDING任务: " + task.getTaskId() + " [" + task.getTaskType() + "]");
                handleTask(task);
            });
            
            // 执行重试任务  
            logger.debug("🔄 开始处理RETRY任务...");
            retryTasks.forEach(task -> {
                logger.info("🔄 调度RETRY任务: " + task.getTaskId() + " [" + task.getTaskType() + "] (第" + task.getRetryTimes() + "次重试)");
                handleTask(task);
            });
            
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