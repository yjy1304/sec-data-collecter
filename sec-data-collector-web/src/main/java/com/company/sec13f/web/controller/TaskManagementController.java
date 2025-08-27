package com.company.sec13f.web.controller;

import com.company.sec13f.repository.model.ScrapingTask;
import com.company.sec13f.repository.enums.TaskStatus;
import com.company.sec13f.service.DataScrapingService;
import com.company.sec13f.service.ScheduledScrapingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Spring MVC Controller for task management
 */
@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "*")
public class TaskManagementController {
    
    private final DataScrapingService scrapingService;
    private final ScheduledScrapingService scheduledService;
    
    @Autowired
    public TaskManagementController(DataScrapingService scrapingService, 
                                   ScheduledScrapingService scheduledService) {
        this.scrapingService = scrapingService;
        this.scheduledService = scheduledService;
    }
    
    /**
     * 获取任务统计信息
     * GET /api/tasks/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getTaskStats() {
        try {
            List<ScrapingTask> allTasks = scrapingService.getAllTaskStatuses();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalTasks", allTasks.size());
            stats.put("pendingTasks", allTasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.PENDING)
                .count());
            stats.put("runningTasks", allTasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.RUNNING)
                .count());
            stats.put("completedTasks", allTasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.COMPLETED)
                .count());
            stats.put("failedTasks", allTasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.FAILED)
                .count());
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Failed to get task statistics: " + e.getMessage()));
        }
    }
    
    /**
     * 获取运行中的任务
     * GET /api/tasks/running
     */
    @GetMapping("/running")
    public ResponseEntity<?> getRunningTasks() {
        try {
            List<ScrapingTask> allTasks = scrapingService.getAllTaskStatuses();
            List<ScrapingTask> runningTasks = allTasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.RUNNING)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(runningTasks);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Failed to get running tasks: " + e.getMessage()));
        }
    }
    
    /**
     * 获取失败的任务
     * GET /api/tasks/failed
     */
    @GetMapping("/failed")
    public ResponseEntity<?> getFailedTasks() {
        try {
            List<ScrapingTask> allTasks = scrapingService.getAllTaskStatuses();
            List<ScrapingTask> failedTasks = allTasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.FAILED)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(failedTasks);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Failed to get failed tasks: " + e.getMessage()));
        }
    }
    
    /**
     * 获取已完成的任务
     * GET /api/tasks/completed
     */
    @GetMapping("/completed")
    public ResponseEntity<?> getCompletedTasks() {
        try {
            List<ScrapingTask> allTasks = scrapingService.getAllTaskStatuses();
            List<ScrapingTask> completedTasks = allTasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.COMPLETED)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(completedTasks);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Failed to get completed tasks: " + e.getMessage()));
        }
    }
    
    /**
     * 手动触发失败任务重试
     * POST /api/tasks/retry-failed
     */
    @PostMapping("/retry-failed")
    public ResponseEntity<?> retryFailedTasks() {
        try {
            Map<String, Object> result = scheduledService.triggerManualRetry();
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Failed to retry failed tasks: " + e.getMessage()));
        }
    }
    
    /**
     * 清理已完成的任务
     * POST /api/tasks/cleanup
     */
    @PostMapping("/cleanup")
    public ResponseEntity<?> cleanupTasks() {
        try {
            scrapingService.cleanupCompletedTasks();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Completed tasks cleaned up successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Failed to cleanup tasks: " + e.getMessage()));
        }
    }
    
    /**
     * 获取任务详细信息
     * GET /api/tasks/{taskId}
     */
    @GetMapping("/{taskId}")
    public ResponseEntity<?> getTaskDetails(@PathVariable String taskId) {
        try {
            ScrapingTask task = scrapingService.getTaskStatus(taskId);
            if (task == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(task);
            
        } catch (Exception e) {
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