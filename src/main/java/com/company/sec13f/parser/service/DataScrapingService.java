package com.company.sec13f.parser.service;

import com.company.sec13f.parser.database.FilingDAO;
import com.company.sec13f.parser.model.Filing;
import com.company.sec13f.parser.scraper.RealSECScraper;
import com.company.sec13f.parser.scraper.FinnhubSECScraper;
import com.company.sec13f.parser.util.DataValidator;
import com.company.sec13f.parser.util.Logger;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DataScrapingService {
    
    private final RealSECScraper scraper; // 切换回SEC官方API
    private final FilingDAO filingDAO;
    private final ExecutorService executorService;
    private final Map<String, ScrapingStatus> scrapingTasks;
    private final Logger logger;
    
    public DataScrapingService(FilingDAO filingDAO) {
        this.scraper = new RealSECScraper(); // 使用修复后的SEC官方API
        this.filingDAO = filingDAO;
        this.executorService = Executors.newFixedThreadPool(3); // 限制并发数以遵守API频率限制
        this.scrapingTasks = new ConcurrentHashMap<>();
        this.logger = Logger.getInstance();
        
        // 记录服务初始化
        logger.info("DataScrapingService initialized with SEC Official API (fixed), thread pool size: 3");
    }
    
    /**
     * 异步爬取指定公司的最新13F数据
     */
    public String scrapeCompanyData(String cik, String companyName) {
        String taskId = generateTaskId(cik);
        
        if (scrapingTasks.containsKey(taskId)) {
            ScrapingStatus existingStatus = scrapingTasks.get(taskId);
            if (existingStatus.getStatus() == TaskStatus.RUNNING) {
                return taskId; // 任务已在进行中
            }
        }
        
        ScrapingStatus status = new ScrapingStatus(taskId, cik, companyName);
        scrapingTasks.put(taskId, status);
        
        CompletableFuture.supplyAsync(() -> {
            try {
                status.setStatus(TaskStatus.RUNNING);
                status.setStartTime(LocalDateTime.now());
                logger.scrapingStarted(cik, companyName);
                
                // 获取公司的13F文件列表
                List<Filing> filings = scraper.getCompanyFilings(cik);
                logger.info("Found " + filings.size() + " 13F filings for " + companyName);
                status.setMessage("找到 " + filings.size() + " 个13F文件");
                
                int savedCount = 0;
                for (Filing filing : filings) {
                    // 检查是否已经存在
                    if (!isFilingExists(filing.getAccessionNumber())) {
                        // 获取详细的13F数据
                        Filing detailedFiling = scraper.get13FDetails(filing.getAccessionNumber(), cik);
                        detailedFiling.setCompanyName(companyName);
                        detailedFiling.setCik(cik);
                        
                        // 验证数据
                        DataValidator.ValidationResult validation = DataValidator.validateFiling(detailedFiling);
                        if (validation.isValid()) {
                            // 保存到数据库
                            filingDAO.saveFiling(detailedFiling);
                            savedCount++;
                            
                            if (validation.hasWarnings()) {
                                status.setMessage("已保存 " + savedCount + "/" + filings.size() + 
                                    " 个文件 (有 " + validation.getAllWarnings().size() + " 个警告)");
                            }
                        } else {
                            status.setMessage("文件 " + filing.getAccessionNumber() + " 验证失败: " + 
                                validation.getAllErrors().get(0));
                        }
                    }
                }
                
                status.setStatus(TaskStatus.COMPLETED);
                status.setEndTime(LocalDateTime.now());
                status.setMessage("成功爬取并保存了 " + savedCount + " 个新的13F文件");
                status.setSavedFilings(savedCount);
                
                return savedCount;
                
            } catch (Exception e) {
                logger.error("Scraping task failed for CIK: " + cik, e);
                status.setStatus(TaskStatus.FAILED);
                status.setEndTime(LocalDateTime.now());
                status.setMessage("爬取失败: " + e.getMessage());
                status.setError(e.getMessage());
                logger.scrapingFailed(cik, e.getMessage());
                return 0;
            }
        }, executorService).whenComplete((result, throwable) -> {
            if (throwable != null) {
                logger.error("CompletableFuture execution failed for CIK: " + cik, throwable);
                status.setStatus(TaskStatus.FAILED);
                status.setEndTime(LocalDateTime.now());
                status.setMessage("异步任务执行失败: " + throwable.getMessage());
                status.setError(throwable.getMessage());
            }
        });
        
        return taskId;
    }
    
    /**
     * 爬取单个最新的13F文件
     */
    public String scrapeLatest13F(String cik, String companyName) {
        String taskId = generateTaskId(cik) + "_latest";
        
        ScrapingStatus status = new ScrapingStatus(taskId, cik, companyName);
        scrapingTasks.put(taskId, status);
        
        CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Starting latest 13F scraping task for CIK: " + cik);
                status.setStatus(TaskStatus.RUNNING);
                status.setStartTime(LocalDateTime.now());
                status.setMessage("正在获取最新13F数据...");
                logger.scrapingStarted(cik, companyName);
                
                Filing latestFiling = scraper.getLatest13F(cik);
                latestFiling.setCompanyName(companyName);
                logger.info("Retrieved latest filing: " + latestFiling.getAccessionNumber());
                
                if (!isFilingExists(latestFiling.getAccessionNumber())) {
                    // 验证数据
                    DataValidator.ValidationResult validation = DataValidator.validateFiling(latestFiling);
                    if (validation.isValid()) {
                        filingDAO.saveFiling(latestFiling);
                        status.setMessage("成功保存最新13F文件，包含 " + 
                            (latestFiling.getHoldings() != null ? latestFiling.getHoldings().size() : 0) + " 个持仓" +
                            (validation.hasWarnings() ? " (有 " + validation.getAllWarnings().size() + " 个警告)" : ""));
                        status.setSavedFilings(1);
                        logger.scrapingCompleted(cik, 1);
                    } else {
                        status.setMessage("最新13F文件验证失败: " + validation.getAllErrors().get(0));
                        status.setSavedFilings(0);
                        logger.error("Validation failed for filing: " + latestFiling.getAccessionNumber());
                    }
                } else {
                    status.setMessage("最新13F文件已存在，无需重复保存");
                    status.setSavedFilings(0);
                    logger.info("Filing already exists: " + latestFiling.getAccessionNumber());
                }
                
                status.setStatus(TaskStatus.COMPLETED);
                status.setEndTime(LocalDateTime.now());
                logger.info("Completed latest 13F scraping task for CIK: " + cik);
                
                return 1;
                
            } catch (Exception e) {
                logger.error("Scraping task failed for CIK: " + cik, e);
                status.setStatus(TaskStatus.FAILED);
                status.setEndTime(LocalDateTime.now());
                status.setMessage("爬取失败: " + e.getMessage());
                status.setError(e.getMessage());
                logger.scrapingFailed(cik, e.getMessage());
                return 0;
            }
        }, executorService).whenComplete((result, throwable) -> {
            if (throwable != null) {
                logger.error("CompletableFuture execution failed for CIK: " + cik, throwable);
                status.setStatus(TaskStatus.FAILED);
                status.setEndTime(LocalDateTime.now());
                status.setMessage("异步任务执行失败: " + throwable.getMessage());
                status.setError(throwable.getMessage());
            }
        });
        
        return taskId;
    }
    
    /**
     * 批量爬取多个公司的数据
     */
    public List<String> scrapeMultipleCompanies(Map<String, String> companies) {
        List<String> taskIds = new ArrayList<>();
        
        for (Map.Entry<String, String> entry : companies.entrySet()) {
            String cik = entry.getKey();
            String companyName = entry.getValue();
            String taskId = scrapeCompanyData(cik, companyName);
            taskIds.add(taskId);
        }
        
        return taskIds;
    }
    
    /**
     * 获取爬取任务状态
     */
    public ScrapingStatus getTaskStatus(String taskId) {
        return scrapingTasks.get(taskId);
    }
    
    /**
     * 获取所有任务状态
     */
    public List<ScrapingStatus> getAllTaskStatuses() {
        return new ArrayList<>(scrapingTasks.values());
    }
    
    /**
     * 清理完成的任务
     */
    public void cleanupCompletedTasks() {
        scrapingTasks.entrySet().removeIf(entry -> {
            ScrapingStatus status = entry.getValue();
            return status.getStatus() == TaskStatus.COMPLETED || status.getStatus() == TaskStatus.FAILED;
        });
    }
    
    /**
     * 检查文件是否已存在
     */
    private boolean isFilingExists(String accessionNumber) {
        try {
            filingDAO.getFilingIdByAccessionNumber(accessionNumber);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
    
    /**
     * 生成任务ID
     */
    private String generateTaskId(String cik) {
        return "scrape_" + cik + "_" + System.currentTimeMillis();
    }
    
    /**
     * 关闭服务
     */
    public void shutdown() {
        try {
            scraper.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        executorService.shutdown();
    }
    
    /**
     * 爬取任务状态枚举
     */
    public enum TaskStatus {
        PENDING, RUNNING, COMPLETED, FAILED
    }
    
    /**
     * 爬取状态类
     */
    public static class ScrapingStatus {
        private final String taskId;
        private final String cik;
        private final String companyName;
        private TaskStatus status;
        private String message;
        private String error;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private int savedFilings;
        
        public ScrapingStatus(String taskId, String cik, String companyName) {
            this.taskId = taskId;
            this.cik = cik;
            this.companyName = companyName;
            this.status = TaskStatus.PENDING;
            this.message = "任务已创建";
            this.savedFilings = 0;
        }
        
        // Getters and Setters
        public String getTaskId() { return taskId; }
        public String getCik() { return cik; }
        public String getCompanyName() { return companyName; }
        public TaskStatus getStatus() { return status; }
        public void setStatus(TaskStatus status) { this.status = status; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        public int getSavedFilings() { return savedFilings; }
        public void setSavedFilings(int savedFilings) { this.savedFilings = savedFilings; }
        
        public long getDurationSeconds() {
            if (startTime == null) return 0;
            LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
            return java.time.Duration.between(startTime, end).getSeconds();
        }
    }
}