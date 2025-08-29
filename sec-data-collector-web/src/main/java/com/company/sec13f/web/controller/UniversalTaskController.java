package com.company.sec13f.web.controller;

import com.company.sec13f.repository.entity.Task;
import com.company.sec13f.repository.enums.TaskStatus;
import com.company.sec13f.repository.enums.TaskType;
import com.company.sec13f.service.TaskService;
import com.company.sec13f.service.plugin.TaskParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 通用任务管理控制器
 * 基于新的插件化任务管理系统
 */
@RestController
@RequestMapping("/api/v2/tasks")
@CrossOrigin(origins = "*")
public class UniversalTaskController {
    
    private final TaskService taskService;
    
    @Autowired
    public UniversalTaskController(TaskService taskService) {
        this.taskService = taskService;
    }
    
    /**
     * 获取所有任务
     * GET /api/v2/tasks
     */
    @GetMapping
    public ResponseEntity<?> getAllTasks() {
        try {
            List<Task> tasks = taskService.getAllTasks();
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Failed to get tasks: " + e.getMessage()));
        }
    }
    
    /**
     * 获取任务统计信息
     * GET /api/v2/tasks/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getTaskStats() {
        try {
            List<Task> allTasks = taskService.getAllTasks();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalTasks", allTasks.size());
            stats.put("pendingTasks", allTasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.PENDING)
                .count());
            stats.put("runningTasks", allTasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.RUNNING)
                .count());
            stats.put("retryTasks", allTasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.RETRY)
                .count());
            stats.put("completedTasks", allTasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.COMPLETED)
                .count());
            stats.put("failedTasks", allTasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.FAILED)
                .count());
            
            // 按任务类型分组统计
            Map<TaskType, Long> tasksByType = allTasks.stream()
                .collect(Collectors.groupingBy(Task::getTaskType, Collectors.counting()));
            stats.put("tasksByType", tasksByType);
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Failed to get task statistics: " + e.getMessage()));
        }
    }
    
    /**
     * 根据状态获取任务
     * GET /api/v2/tasks?status=RUNNING
     */
    @GetMapping(params = "status")
    public ResponseEntity<?> getTasksByStatus(@RequestParam TaskStatus status) {
        try {
            List<Task> allTasks = taskService.getAllTasks();
            List<Task> filteredTasks = allTasks.stream()
                .filter(task -> task.getStatus() == status)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(filteredTasks);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Failed to get tasks by status: " + e.getMessage()));
        }
    }
    
    /**
     * 根据类型获取任务
     * GET /api/v2/tasks?type=SCRAP_HOLDING
     */
    @GetMapping(params = "type")
    public ResponseEntity<?> getTasksByType(@RequestParam TaskType type) {
        try {
            List<Task> allTasks = taskService.getAllTasks();
            List<Task> filteredTasks = allTasks.stream()
                .filter(task -> task.getTaskType() == type)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(filteredTasks);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Failed to get tasks by type: " + e.getMessage()));
        }
    }
    
    /**
     * 获取任务详细信息
     * GET /api/v2/tasks/{taskId}
     */
    @GetMapping("/{taskId}")
    public ResponseEntity<?> getTaskDetails(@PathVariable String taskId) {
        try {
            Task task = taskService.getTaskStatus(taskId);
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
     * 创建数据抓取任务
     * POST /api/v2/tasks/scraping
     * Body: {"cik": "0001524258", "companyName": "Alibaba Group"}
     */
    @PostMapping("/scraping")
    public ResponseEntity<?> createScrapingTask(@RequestBody Map<String, String> request) {
        try {
            String cik = request.get("cik");
            String companyName = request.get("companyName");
            
            if (cik == null || companyName == null) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Missing required parameters: cik, companyName"));
            }
            
            // 创建任务参数
            TaskParameters params = TaskParameters.forScraping(cik, companyName);
            
            // 创建任务
            String taskId = taskService.createTask(TaskType.SCRAP_HOLDING, params.toJson());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("taskId", taskId);
            response.put("message", "Scraping task created successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Failed to create scraping task: " + e.getMessage()));
        }
    }
    
    /**
     * 创建通用任务
     * POST /api/v2/tasks
     * Body: {"type": "SCRAP_HOLDING", "parameters": {...}}
     */
    @PostMapping
    public ResponseEntity<?> createTask(@RequestBody Map<String, Object> request) {
        try {
            String typeStr = (String) request.get("type");
            if (typeStr == null) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Missing required parameter: type"));
            }
            
            TaskType taskType;
            try {
                taskType = TaskType.valueOf(typeStr);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Invalid task type: " + typeStr));
            }
            
            Object parameters = request.get("parameters");
            String parametersJson = parameters != null ? 
                new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(parameters) : "{}";
            
            String taskId = taskService.createTask(taskType, parametersJson);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("taskId", taskId);
            response.put("message", "Task created successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Failed to create task: " + e.getMessage()));
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