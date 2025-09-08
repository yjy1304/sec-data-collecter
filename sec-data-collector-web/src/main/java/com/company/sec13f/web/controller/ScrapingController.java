package com.company.sec13f.web.controller;

import com.company.sec13f.repository.entity.Task;
import com.company.sec13f.repository.enums.TaskType;
import com.company.sec13f.repository.mapper.TaskMapper;
import com.company.sec13f.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring MVC Controller for data scraping APIs - 使用TaskService统一调度
 */
@RestController
@RequestMapping("/api/scraping")
@CrossOrigin(origins = "*")
public class ScrapingController {
    
    private static final Logger logger = LoggerFactory.getLogger(ScrapingController.class);
    
    @Autowired
    private TaskMapper taskMapper;
    
    @Autowired
    private TaskService taskService;
    
    /**
     * 创建数据爬取任务 - 统一使用TaskService调度
     * POST /api/scraping/scrape
     */
    @PostMapping("/scrape")
    public ResponseEntity<?> scrapeCompany(
            @RequestParam String cik,
            @RequestParam(required = false) String companyName) {
        try {
            if (cik == null || cik.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("CIK parameter is required"));
            }
            
            // 如果没有提供公司名称，使用空字符串或者CIK作为默认值
            String finalCompanyName = (companyName != null && !companyName.trim().isEmpty()) ? 
                companyName.trim() : ("Company_" + cik.trim());
            
            // 创建任务参数
            String taskParameters = String.format("{\"cik\":\"%s\",\"companyName\":\"%s\"}", 
                                                cik.trim(), finalCompanyName);
            
            // 通过TaskService统一创建任务（会自动设置为PENDING状态）
            String taskId = taskService.createTask(TaskType.SCRAP_HOLDING, taskParameters);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("taskId", taskId);
            response.put("message", "任务已创建，将由调度器自动执行");
            response.put("note", "任务状态可通过 /api/scraping/status/{taskId} 查询");
            
            logger.info("📝 创建抓取任务: {} for CIK: {} - {} (状态: PENDING)", taskId, cik, finalCompanyName);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Failed to create scraping task", e);
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Failed to create scraping task: " + e.getMessage()));
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
     * 获取TaskService插件状态（调试用）
     */
    @GetMapping("/plugin-status")
    public ResponseEntity<?> getPluginStatus() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("pluginStatus", taskService.getPluginStatus());
            response.put("registeredPlugins", taskService.getRegisteredPlugins());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Failed to get plugin status: " + e.getMessage()));
        }
    }
}