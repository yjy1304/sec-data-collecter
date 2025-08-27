package com.company.sec13f.web.controller;

import com.company.sec13f.repository.model.ScrapingTask;
import com.company.sec13f.repository.enums.TaskStatus;
import com.company.sec13f.service.DataScrapingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring MVC Controller for data scraping APIs
 */
@RestController
@RequestMapping("/api/scraping")
@CrossOrigin(origins = "*")
public class ScrapingController {
    
    private final DataScrapingService scrapingService;
    
    @Autowired
    public ScrapingController(DataScrapingService scrapingService) {
        this.scrapingService = scrapingService;
    }
    
    /**
     * 启动单个公司的数据爬取任务
     * POST /api/scraping/scrape
     */
    @PostMapping("/scrape")
    public ResponseEntity<?> scrapeCompany(
            @RequestParam String cik,
            @RequestParam String companyName) {
        try {
            if (cik == null || cik.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("CIK parameter is required"));
            }
            
            if (companyName == null || companyName.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Company name parameter is required"));
            }
            
            String taskId = scrapingService.scrapeCompanyData(cik.trim(), companyName.trim());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("taskId", taskId);
            response.put("message", "Scraping task started successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Failed to start scraping task: " + e.getMessage()));
        }
    }
    
    /**
     * 启动最新13F文件爬取任务
     * POST /api/scraping/scrape-latest
     */
    @PostMapping("/scrape-latest")
    public ResponseEntity<?> scrapeLatest13F(
            @RequestParam String cik,
            @RequestParam String companyName) {
        try {
            if (cik == null || cik.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("CIK parameter is required"));
            }
            
            if (companyName == null || companyName.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Company name parameter is required"));
            }
            
            String taskId = scrapingService.scrapeLatest13F(cik.trim(), companyName.trim());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("taskId", taskId);
            response.put("message", "Latest 13F scraping task started successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Failed to start scraping task: " + e.getMessage()));
        }
    }
    
    /**
     * 批量爬取多个公司数据
     * POST /api/scraping/scrape-batch
     */
    @PostMapping("/scrape-batch")
    public ResponseEntity<?> scrapeBatch(@RequestBody Map<String, String> companies) {
        try {
            if (companies == null || companies.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Company list is required"));
            }
            
            List<String> taskIds = scrapingService.scrapeMultipleCompanies(companies);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("taskIds", taskIds);
            response.put("message", "Batch scraping tasks started successfully");
            response.put("count", taskIds.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Failed to start batch scraping: " + e.getMessage()));
        }
    }
    
    /**
     * 获取任务状态
     * GET /api/scraping/status?taskId=xxx
     */
    @GetMapping("/status")
    public ResponseEntity<?> getTaskStatus(@RequestParam String taskId) {
        try {
            if (taskId == null || taskId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Task ID parameter is required"));
            }
            
            ScrapingTask task = scrapingService.getTaskStatus(taskId.trim());
            if (task == null) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("taskId", task.getTaskId());
            response.put("cik", task.getCik());
            response.put("companyName", task.getCompanyName());
            response.put("status", task.getStatus().toString());
            response.put("message", task.getMessage());
            response.put("startTime", task.getStartTime());
            response.put("endTime", task.getEndTime());
            response.put("savedFilings", task.getSavedFilings());
            
            if (task.getStatus() == TaskStatus.FAILED) {
                response.put("error", task.getError());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Failed to get task status: " + e.getMessage()));
        }
    }
    
    /**
     * 获取所有任务状态
     * GET /api/scraping/tasks
     */
    @GetMapping("/tasks")
    public ResponseEntity<?> getAllTasks() {
        try {
            List<ScrapingTask> tasks = scrapingService.getAllTaskStatuses();
            return ResponseEntity.ok(tasks);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Failed to get tasks: " + e.getMessage()));
        }
    }
    
    /**
     * 清理已完成的任务
     * DELETE /api/scraping/cleanup
     */
    @DeleteMapping("/cleanup")
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