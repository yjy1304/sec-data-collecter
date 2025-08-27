package com.company.sec13f.service;

import com.company.sec13f.repository.database.TaskDAO;
import com.company.sec13f.repository.model.ScrapingTask;
import com.company.sec13f.repository.enums.TaskStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Service
public class ScheduledScrapingService {
    
    private static final Logger logger = Logger.getLogger(ScheduledScrapingService.class.getName());
    
    private final DataScrapingService dataScrapingService;
    private final TaskDAO taskDAO;
    
    // 知名公司的CIK和名称映射
    private static final Map<String, String> MAJOR_COMPANIES = new HashMap<>();
    static {
        MAJOR_COMPANIES.put("0001067983", "Berkshire Hathaway Inc");
        MAJOR_COMPANIES.put("0001013594", "BlackRock Inc");
        MAJOR_COMPANIES.put("0000909832", "Vanguard Group Inc");
        MAJOR_COMPANIES.put("0001364742", "State Street Corp");
        MAJOR_COMPANIES.put("0001166559", "JPMorgan Chase & Co");
        MAJOR_COMPANIES.put("0000019617", "Goldman Sachs Group Inc");
        MAJOR_COMPANIES.put("0000831001", "Morgan Stanley");
        MAJOR_COMPANIES.put("0001524258", "Alibaba Group Holding Limited");
    }
    
    public ScheduledScrapingService(DataScrapingService dataScrapingService, TaskDAO taskDAO) {
        this.dataScrapingService = dataScrapingService;
        this.taskDAO = taskDAO;
    }
    
    /**
     * 定期爬取主要公司的最新13F文件
     * 每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledDailyDataCollection() {
        logger.info("Starting scheduled daily data collection for major companies");
        
        int successCount = 0;
        int failureCount = 0;
        
        for (Map.Entry<String, String> company : MAJOR_COMPANIES.entrySet()) {
            try {
                String taskId = scrapeCompanyDataWithRetry(company.getKey(), company.getValue());
                successCount++;
                logger.info("Successfully started scraping task for " + company.getValue() + " (CIK: " + company.getKey() + "), Task ID: " + taskId);
            } catch (Exception e) {
                failureCount++;
                logger.severe("Failed to start scraping task for " + company.getValue() + " (CIK: " + company.getKey() + "): " + e.getMessage());
            }
            
            // 在公司之间添加延迟，避免对SEC服务器造成过大压力
            try {
                Thread.sleep(5000); // 5秒延迟
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warning("Thread interrupted during delay: " + e.getMessage());
            }
        }
        
        logger.info("Scheduled daily data collection completed. Success: " + successCount + ", Failures: " + failureCount);
    }
    
    /**
     * 重试失败的任务
     * 每小时执行一次
     */
    @Scheduled(fixedRate = 3600000) // 每小时
    public void retryFailedTasks() {
        logger.info("Starting retry of failed tasks");
        
        try {
            List<ScrapingTask> failedTasks = getFailedTasksForRetry();
            
            if (failedTasks.isEmpty()) {
                logger.info("No failed tasks found for retry");
                return;
            }
            
            logger.info("Found " + failedTasks.size() + " failed tasks for retry");
            
            for (ScrapingTask failedTask : failedTasks) {
                try {
                    retryFailedTaskWithBackoff(failedTask);
                    logger.info("Successfully retried task: " + failedTask.getTaskId());
                } catch (Exception e) {
                    logger.severe("Failed to retry task " + failedTask.getTaskId() + ": " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            logger.severe("Error during failed tasks retry: " + e.getMessage());
        }
    }
    
    /**
     * 使用重试机制爬取公司数据
     */
    @Retryable(
        value = {Exception.class}, 
        maxAttempts = 3, 
        backoff = @Backoff(delay = 2000, multiplier = 2.0)
    )
    public String scrapeCompanyDataWithRetry(String cik, String companyName) {
        logger.info("Attempting to scrape data for company: " + companyName + " (CIK: " + cik + ")");
        
        try {
            return dataScrapingService.scrapeLatest13F(cik, companyName);
        } catch (Exception e) {
            logger.warning("Scraping attempt failed for " + companyName + ": " + e.getMessage());
            throw e; // 重新抛出异常以触发重试
        }
    }
    
    /**
     * 重试失败的任务
     */
    @Retryable(
        value = {Exception.class}, 
        maxAttempts = 2, 
        backoff = @Backoff(delay = 5000, multiplier = 1.5)
    )
    public void retryFailedTaskWithBackoff(ScrapingTask failedTask) {
        logger.info("Retrying failed task: " + failedTask.getTaskId() + " for company: " + failedTask.getCompanyName());
        
        try {
            // 重新启动任务
            String newTaskId = dataScrapingService.scrapeLatest13F(failedTask.getCik(), failedTask.getCompanyName());
            logger.info("Created retry task with ID: " + newTaskId + " for original task: " + failedTask.getTaskId());
        } catch (Exception e) {
            logger.warning("Retry attempt failed for task " + failedTask.getTaskId() + ": " + e.getMessage());
            throw e; // 重新抛出异常以触发重试
        }
    }
    
    /**
     * 获取需要重试的失败任务
     * 只重试最近24小时内失败的任务
     */
    private List<ScrapingTask> getFailedTasksForRetry() throws SQLException {
        // 从数据库获取失败的任务
        List<ScrapingTask> allTasks = taskDAO.getAllTasks();
        
        return allTasks.stream()
            .filter(task -> task.getStatus() == TaskStatus.FAILED)
            .filter(task -> {
                // 只重试最近24小时内失败的任务
                if (task.getEndTime() == null) return false;
                
                long hoursSinceFailure = java.time.Duration.between(
                    task.getEndTime(), 
                    java.time.LocalDateTime.now()
                ).toHours();
                
                return hoursSinceFailure <= 24;
            })
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * 手动触发所有主要公司的数据爬取
     */
    public Map<String, Object> triggerManualDataCollection() {
        logger.info("Manual data collection triggered");
        
        Map<String, Object> result = new HashMap<>();
        Map<String, String> taskIds = new HashMap<>();
        int successCount = 0;
        int failureCount = 0;
        
        for (Map.Entry<String, String> company : MAJOR_COMPANIES.entrySet()) {
            try {
                String taskId = scrapeCompanyDataWithRetry(company.getKey(), company.getValue());
                taskIds.put(company.getValue(), taskId);
                successCount++;
            } catch (Exception e) {
                taskIds.put(company.getValue(), "Failed: " + e.getMessage());
                failureCount++;
            }
        }
        
        result.put("success", true);
        result.put("message", "Manual data collection completed");
        result.put("successCount", successCount);
        result.put("failureCount", failureCount);
        result.put("taskIds", taskIds);
        
        return result;
    }
    
    /**
     * 手动触发失败任务重试
     */
    public Map<String, Object> triggerManualRetry() {
        logger.info("Manual retry triggered");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<ScrapingTask> failedTasks = getFailedTasksForRetry();
            
            if (failedTasks.isEmpty()) {
                result.put("success", true);
                result.put("message", "No failed tasks found for retry");
                result.put("retriedCount", 0);
                return result;
            }
            
            int retriedCount = 0;
            for (ScrapingTask failedTask : failedTasks) {
                try {
                    retryFailedTaskWithBackoff(failedTask);
                    retriedCount++;
                } catch (Exception e) {
                    logger.severe("Manual retry failed for task " + failedTask.getTaskId() + ": " + e.getMessage());
                }
            }
            
            result.put("success", true);
            result.put("message", "Manual retry completed");
            result.put("totalFailedTasks", failedTasks.size());
            result.put("retriedCount", retriedCount);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Manual retry failed: " + e.getMessage());
        }
        
        return result;
    }
}