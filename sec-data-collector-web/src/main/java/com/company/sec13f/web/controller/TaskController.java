package com.company.sec13f.web.controller;

import com.company.sec13f.repository.mapper.TaskMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 任务统计和管理控制器
 */
@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "*")
public class TaskController {
    
    @Autowired
    private TaskMapper taskMapper;
    
    /**
     * 获取任务统计信息
     * GET /api/tasks/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getTaskStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 获取总任务数
            long totalTasks = taskMapper.countAll();
            
            // 获取各状态的任务数量
            long pendingTasks = taskMapper.countByStatus("PENDING");
            long runningTasks = taskMapper.countByStatus("RUNNING");
            long completedTasks = taskMapper.countByStatus("COMPLETED");
            long failedTasks = taskMapper.countByStatus("FAILED");
            long retryTasks = taskMapper.countByStatus("RETRY");
            
            stats.put("totalTasks", totalTasks);
            stats.put("pendingTasks", pendingTasks);
            stats.put("runningTasks", runningTasks);
            stats.put("completedTasks", completedTasks);
            stats.put("failedTasks", failedTasks);
            stats.put("retryTasks", retryTasks);
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Failed to get task statistics: " + e.getMessage()));
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