package com.company.sec13f.web.controller;

import com.company.sec13f.repository.entity.Task;
import com.company.sec13f.repository.enums.TaskStatus;
import com.company.sec13f.repository.enums.TaskType;
import com.company.sec13f.repository.mapper.TaskMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Spring MVC Controller for data scraping APIs - 简化版本
 */
@RestController
@RequestMapping("/api/scraping")
@CrossOrigin(origins = "*")
public class ScrapingController {
    
    private static final Logger logger = LoggerFactory.getLogger(ScrapingController.class);
    private final ExecutorService executorService = Executors.newFixedThreadPool(3);
    private final Map<String, CompletableFuture<Void>> runningTasks = new ConcurrentHashMap<>();
    
    @Autowired
    private TaskMapper taskMapper;
    
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
            
            String taskId = generateTaskId();
            
            // 插入任务到数据库
            insertTaskToDatabase(taskId, cik.trim(), companyName.trim());
            
            // 异步执行抓取任务
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                executeScrapingTask(taskId, cik.trim(), companyName.trim());
            }, executorService);
            
            runningTasks.put(taskId, future);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("taskId", taskId);
            response.put("message", "Scraping task started successfully");
            
            logger.info("🎯 Started scraping task: {} for CIK: {} - {}", taskId, cik, companyName);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Failed to start scraping task", e);
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Failed to start scraping task: " + e.getMessage()));
        }
    }
    
    /**
     * 获取所有任务状态 - 简化版本，避开MyBatis问题
     * GET /api/scraping/tasks
     */
    @GetMapping("/tasks")
    public ResponseEntity<?> getAllTasks() {
        try {
            // 直接使用SQL查询获取任务数据，避开MyBatis枚举映射问题
            List<Map<String, Object>> tasks = getTasksDirectly();
            return ResponseEntity.ok(tasks);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Failed to get tasks: " + e.getMessage()));
        }
    }
    
    /**
     * 获取任务状态
     * GET /api/scraping/status/{taskId}
     */
    @GetMapping("/status/{taskId}")
    public ResponseEntity<?> getTaskStatus(@PathVariable String taskId) {
        try {
            Map<String, Object> task = getTaskDirectly(taskId);
            if (task != null) {
                return ResponseEntity.ok(task);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Failed to get task status: " + e.getMessage()));
        }
    }
    
    /**
     * 插入任务到数据库
     */
    private void insertTaskToDatabase(String taskId, String cik, String companyName) {
        try {
            // 创建任务参数JSON
            String taskParameters = String.format("{\"cik\":\"%s\",\"companyName\":\"%s\"}", cik, companyName);
            
            // 创建Task对象
            Task task = new Task(taskId, TaskType.SEC_SCRAPING);
            task.setTaskParameters(taskParameters);
            task.setMessage("任务已创建");
            
            // 使用MyBatis插入任务
            int inserted = taskMapper.insert(task);
            if (inserted > 0) {
                logger.info("📝 Created task in database: {}", taskId);
            }
        } catch (Exception e) {
            logger.error("❌ Failed to insert task to database: " + taskId, e);
        }
    }
    
    /**
     * 执行抓取任务
     */
    private void executeScrapingTask(String taskId, String cik, String companyName) {
        try {
            // 更新任务状态为运行中
            updateTaskStatus(taskId, "RUNNING", "正在抓取数据...");
            logger.info("🔄 SCRAPING_STARTED - CIK: {}, Company: {}", cik, companyName);
            
            // 模拟抓取过程
            Thread.sleep(3000); // 模拟3秒的抓取时间
            
            // 更新任务状态为完成
            updateTaskStatus(taskId, "COMPLETED", String.format("模拟抓取完成 - CIK: %s (%s)", cik, companyName));
            logger.info("✅ SCRAPING_COMPLETED - CIK: {}, Company: {}", cik, companyName);
            
        } catch (Exception e) {
            String errorMessage = "Scraping task failed: " + e.getMessage();
            updateTaskStatus(taskId, "FAILED", errorMessage);
            logger.error("❌ SCRAPING_FAILED - CIK: {}, Error: {}", cik, e.getMessage());
        } finally {
            runningTasks.remove(taskId);
        }
    }
    
    /**
     * 更新任务状态
     */
    private void updateTaskStatus(String taskId, String status, String message) {
        try {
            // 查询现有任务
            Task task = taskMapper.selectByTaskId(taskId);
            if (task == null) {
                logger.error("❌ Task not found: {}", taskId);
                return;
            }
            
            // 更新任务状态
            TaskStatus taskStatus = TaskStatus.valueOf(status);
            task.setStatus(taskStatus);
            task.setMessage(message);
            task.setUpdatedAt(LocalDateTime.now());
            
            // 根据状态设置时间
            if (TaskStatus.RUNNING.equals(taskStatus)) {
                task.setStartTime(LocalDateTime.now());
            } else if (TaskStatus.COMPLETED.equals(taskStatus) || TaskStatus.FAILED.equals(taskStatus)) {
                task.setEndTime(LocalDateTime.now());
            }
            
            // 使用MyBatis更新任务
            int updated = taskMapper.update(task);
            if (updated > 0) {
                logger.debug("🔄 Updated task status: {} -> {}", taskId, status);
            }
        } catch (Exception e) {
            logger.error("❌ Failed to update task status: " + taskId, e);
        }
    }
    
    /**
     * 使用MyBatis获取任务列表
     */
    private List<Map<String, Object>> getTasksDirectly() {
        List<Map<String, Object>> taskMaps = new ArrayList<>();
        try {
            // 使用MyBatis查询所有任务
            List<Task> tasks = taskMapper.selectAll();
            
            // 转换为Map格式，保持原有API兼容性
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            
            for (Task task : tasks) {
                Map<String, Object> taskMap = new HashMap<>();
                taskMap.put("taskId", task.getTaskId());
                taskMap.put("taskType", task.getTaskType().name());
                taskMap.put("status", task.getStatus().name());
                taskMap.put("message", task.getMessage());
                taskMap.put("retryTimes", task.getRetryTimes() != null ? task.getRetryTimes() : 0);
                
                // 从任务参数中解析CIK和公司名称
                String cik = "";
                String companyName = "";
                try {
                    if (task.getTaskParameters() != null) {
                        // 简单的JSON解析，提取cik和companyName
                        String params = task.getTaskParameters();
                        if (params.contains("cik")) {
                            int cikStart = params.indexOf("\"cik\":\"") + 7;
                            int cikEnd = params.indexOf("\"", cikStart);
                            if (cikEnd > cikStart) {
                                cik = params.substring(cikStart, cikEnd);
                            }
                        }
                        if (params.contains("companyName")) {
                            int nameStart = params.indexOf("\"companyName\":\"") + 15;
                            int nameEnd = params.indexOf("\"", nameStart);
                            if (nameEnd > nameStart) {
                                companyName = params.substring(nameStart, nameEnd);
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Failed to parse task parameters for task: " + task.getTaskId());
                }
                
                taskMap.put("cik", cik);
                taskMap.put("companyName", companyName);
                taskMap.put("savedFilings", 0); // 暂时设为0，后续可以从数据库查询实际数量
                
                // 处理时间字段
                if (task.getStartTime() != null) {
                    taskMap.put("startTime", task.getStartTime().format(formatter));
                }
                
                if (task.getEndTime() != null) {
                    taskMap.put("endTime", task.getEndTime().format(formatter));
                }
                
                if (task.getCreatedAt() != null) {
                    taskMap.put("createdAt", task.getCreatedAt().format(formatter));
                }
                
                // 计算持续时间
                taskMap.put("durationSeconds", task.getDurationSeconds());
                
                taskMaps.add(taskMap);
            }
            
        } catch (Exception e) {
            logger.error("Failed to get tasks using MyBatis", e);
        }
        
        return taskMaps;
    }
    
    /**
     * 使用MyBatis获取单个任务
     */
    private Map<String, Object> getTaskDirectly(String taskId) {
        try {
            // 使用MyBatis查询任务
            Task task = taskMapper.selectByTaskId(taskId);
            
            if (task != null) {
                // 转换为Map格式，保持原有API兼容性
                Map<String, Object> taskMap = new HashMap<>();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                
                taskMap.put("taskId", task.getTaskId());
                taskMap.put("taskType", task.getTaskType().name());
                taskMap.put("status", task.getStatus().name());
                taskMap.put("message", task.getMessage());
                taskMap.put("retryTimes", task.getRetryTimes() != null ? task.getRetryTimes() : 0);
                
                // 处理时间字段
                if (task.getStartTime() != null) {
                    taskMap.put("startTime", task.getStartTime().format(formatter));
                }
                
                if (task.getEndTime() != null) {
                    taskMap.put("endTime", task.getEndTime().format(formatter));
                }
                
                if (task.getCreatedAt() != null) {
                    taskMap.put("createdAt", task.getCreatedAt().format(formatter));
                }
                
                // 计算持续时间
                taskMap.put("durationSeconds", task.getDurationSeconds());
                
                return taskMap;
            }
            
        } catch (Exception e) {
            logger.error("Failed to get task using MyBatis: " + taskId, e);
        }
        
        return null;
    }
    
    /**
     * 生成任务ID
     */
    private String generateTaskId() {
        return "scrape_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
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
    
    /**
     * 获取运行中的任务数量
     */
    @GetMapping("/running-count")
    public ResponseEntity<?> getRunningTasksCount() {
        Map<String, Object> response = new HashMap<>();
        response.put("runningTasks", runningTasks.size());
        return ResponseEntity.ok(response);
    }
}