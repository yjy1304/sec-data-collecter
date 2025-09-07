package com.company.sec13f.web.controller;

import com.company.sec13f.repository.entity.Task;
import com.company.sec13f.repository.enums.TaskType;
import com.company.sec13f.service.TaskService;
import com.company.sec13f.service.plugin.TaskParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 任务系统调试和测试控制器
 */
@RestController
@RequestMapping("/api/tasks/debug")
@CrossOrigin(origins = "*")
public class TaskDebugController {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskDebugController.class);
    
    @Autowired
    private TaskService taskService;
    
    /**
     * 获取TaskService状态信息
     * GET /api/tasks/debug/status
     */
    @GetMapping("/status")
    public ResponseEntity<?> getTaskServiceStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            
            // 插件注册状态
            status.put("registeredPlugins", taskService.getRegisteredPlugins());
            status.put("pluginStatusDetails", taskService.getPluginStatus());
            
            // 检查具体的插件
            status.put("scrapingPluginRegistered", taskService.isPluginRegistered(TaskType.SCRAP_HOLDING));
            
            logger.info("📊 TaskService状态查询: " + status);
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            logger.error("❌ Failed to get TaskService status", e);
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Failed to get TaskService status: " + e.getMessage()));
        }
    }
    
    /**
     * 创建测试任务
     * POST /api/tasks/debug/create-test-task
     */
    @PostMapping("/create-test-task")
    public ResponseEntity<?> createTestTask() {
        try {
            // 创建测试任务参数
            TaskParameters params = TaskParameters.forScraping("0001524258", "Alibaba Group (测试)");
            
            // 创建任务
            String taskId = taskService.createTask(TaskType.SCRAP_HOLDING, params.toJson());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("taskId", taskId);
            response.put("message", "测试任务已创建");
            response.put("taskParameters", params.toJson());
            
            logger.info("🧪 创建测试任务: " + taskId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Failed to create test task", e);
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Failed to create test task: " + e.getMessage()));
        }
    }
    
    /**
     * 手动触发任务调度（测试用）
     * POST /api/tasks/debug/trigger-schedule
     */
    @PostMapping("/trigger-schedule")
    public ResponseEntity<?> triggerSchedule() {
        try {
            logger.info("🔧 手动触发任务调度...");
            
            // 直接调用调度方法
            taskService.scheduleTask();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "任务调度已手动触发");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Failed to trigger schedule", e);
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Failed to trigger schedule: " + e.getMessage()));
        }
    }
    
    /**
     * 获取指定任务的详细信息
     * GET /api/tasks/debug/{taskId}
     */
    @GetMapping("/{taskId}")
    public ResponseEntity<?> getTaskDetails(@PathVariable String taskId) {
        try {
            Task task = taskService.getTaskStatus(taskId);
            if (task == null) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("taskId", task.getTaskId());
            response.put("taskType", task.getTaskType());
            response.put("status", task.getStatus());
            response.put("message", task.getMessage());
            response.put("retryTimes", task.getRetryTimes());
            response.put("durationSeconds", task.getDurationSeconds());
            response.put("taskParameters", task.getTaskParameters());
            response.put("startTime", task.getStartTime());
            response.put("endTime", task.getEndTime());
            response.put("createdAt", task.getCreatedAt());
            response.put("updatedAt", task.getUpdatedAt());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Failed to get task details for: " + taskId, e);
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Failed to get task details: " + e.getMessage()));
        }
    }
    
    /**
     * 创建错误响应
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", true);
        error.put("message", message);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }
}