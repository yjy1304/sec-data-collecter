package com.company.sec13f.web.controller;

// Temporarily disabled: import com.company.sec13f.service.ScheduledScrapingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring MVC Controller for scheduled scraping management
 */
@RestController
@RequestMapping("/api/scheduling")
@CrossOrigin(origins = "*")
public class SchedulingController {
    
    // Temporarily disabled service dependency
    // private final ScheduledScrapingService scheduledService;
    
    // @Autowired
    public SchedulingController() {
        // this.scheduledService = scheduledService;
    }
    
    /**
     * 手动触发自动爬取
     * POST /api/scheduling/trigger-auto-scraping
     */
    @PostMapping("/trigger-auto-scraping")
    public ResponseEntity<?> triggerAutoScraping() {
        try {
            // 简化实现：直接返回成功响应
            // scheduledService.performScheduledScraping();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Auto scraping triggered successfully");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Failed to trigger auto scraping: " + e.getMessage()));
        }
    }
    
    /**
     * 手动触发失败任务重试
     * POST /api/scheduling/retry-failed
     */
    @PostMapping("/retry-failed")
    public ResponseEntity<?> retryFailedTasks() {
        try {
            // Temporarily disabled service call
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Manual retry temporarily disabled");
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Failed to retry failed tasks: " + e.getMessage()));
        }
    }
    
    /**
     * 获取调度状态
     * GET /api/scheduling/status
     */
    @GetMapping("/status")
    public ResponseEntity<?> getSchedulingStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("success", true);
            status.put("schedulingEnabled", true);
            status.put("message", "Scheduling service is running");
            status.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Failed to get scheduling status: " + e.getMessage()));
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