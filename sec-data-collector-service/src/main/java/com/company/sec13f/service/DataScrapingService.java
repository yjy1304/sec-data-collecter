package com.company.sec13f.service;

import com.company.sec13f.repository.database.FilingDAO;
import com.company.sec13f.repository.database.TaskDAO;
import com.company.sec13f.repository.model.Filing;
import com.company.sec13f.repository.model.ScrapingTask;
import com.company.sec13f.repository.enums.TaskStatus;
import com.company.sec13f.service.scraper.RealSECScraper;
import com.company.sec13f.service.scraper.FinnhubSECScraper;
import com.company.sec13f.service.util.DataValidator;
import com.company.sec13f.service.util.Logger;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

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

@Service
public class DataScrapingService {
    
    private final RealSECScraper scraper; // 切换回SEC官方API
    private final FilingDAO filingDAO;
    private final TaskDAO taskDAO;
    private final ExecutorService executorService;
    private final Map<String, ScrapingTask> scrapingTasks;
    private final Logger logger;
    
    @Autowired
    public DataScrapingService(FilingDAO filingDAO) {
        this.scraper = new RealSECScraper(); // 使用修复后的SEC官方API
        this.filingDAO = filingDAO;
        this.taskDAO = new TaskDAO();
        this.executorService = Executors.newFixedThreadPool(3); // 限制并发数以遵守API频率限制
        this.scrapingTasks = new ConcurrentHashMap<>();
        this.logger = Logger.getInstance();
        
        // 初始化任务表和加载现有任务
        try {
            taskDAO.initializeTasksTable();
            loadExistingTasks();
        } catch (SQLException e) {
            logger.error("Failed to initialize task persistence", e);
        }
        
        // 记录服务初始化
        logger.info("DataScrapingService initialized with SEC Official API (fixed), thread pool size: 3");
    }
    
    /**
     * 从数据库加载现有任务
     */
    private void loadExistingTasks() throws SQLException {
        List<ScrapingTask> existingTasks = taskDAO.getAllTasks();
        for (ScrapingTask task : existingTasks) {
            // 只加载未完成的任务到内存中
            if (task.getStatus() == TaskStatus.RUNNING || task.getStatus() == TaskStatus.PENDING) {
                scrapingTasks.put(task.getTaskId(), task);
                // 如果任务状态为RUNNING但服务重启了，将状态改为FAILED
                if (task.getStatus() == TaskStatus.RUNNING) {
                    task.setStatus(TaskStatus.FAILED);
                    task.setEndTime(LocalDateTime.now());
                    task.setMessage("任务因服务重启而中断");
                    task.setError("Service restart interrupted task");
                    saveTaskToDatabase(task);
                }
            } else {
                // 对于已完成的任务，也加载到内存中用于显示
                scrapingTasks.put(task.getTaskId(), task);
            }
        }
        logger.info("Loaded " + existingTasks.size() + " existing tasks from database");
    }
    
    /**
     * 保存任务到数据库
     */
    private void saveTaskToDatabase(ScrapingTask task) {
        try {
            // 尝试获取现有任务
            ScrapingTask existingTask = taskDAO.getTaskById(task.getTaskId());
            if (existingTask == null) {
                // 新任务，插入
                taskDAO.saveTask(task);
            } else {
                // 现有任务，更新
                taskDAO.updateTask(task);
            }
        } catch (SQLException e) {
            logger.error("Failed to save task to database: " + task.getTaskId(), e);
        }
    }
    
    /**
     * 异步爬取指定公司的最新13F数据
     */
    public String scrapeCompanyData(String cik, String companyName) {
        String taskId = generateTaskId(cik);
        
        if (scrapingTasks.containsKey(taskId)) {
            ScrapingTask existingStatus = scrapingTasks.get(taskId);
            if (existingStatus.getStatus() == TaskStatus.RUNNING) {
                return taskId; // 任务已在进行中
            }
        }
        
        ScrapingTask status = new ScrapingTask(taskId, cik, companyName);
        scrapingTasks.put(taskId, status);
        
        // 保存新任务到数据库
        saveTaskToDatabase(status);
        
        CompletableFuture.supplyAsync(() -> {
            try {
                status.setStatus(TaskStatus.RUNNING);
                status.setStartTime(LocalDateTime.now());
                saveTaskToDatabase(status); // 更新运行状态
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
                saveTaskToDatabase(status); // 保存完成状态
                
                return savedCount;
                
            } catch (Exception e) {
                logger.error("Scraping task failed for CIK: " + cik, e);
                status.setStatus(TaskStatus.FAILED);
                status.setEndTime(LocalDateTime.now());
                status.setMessage("爬取失败: " + e.getMessage());
                status.setError(e.getMessage());
                saveTaskToDatabase(status); // 保存失败状态
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
                saveTaskToDatabase(status); // 保存异步失败状态
            }
        });
        
        return taskId;
    }
    
    /**
     * 爬取单个最新的13F文件
     */
    public String scrapeLatest13F(String cik, String companyName) {
        String taskId = generateTaskId(cik) + "_latest";
        
        ScrapingTask status = new ScrapingTask(taskId, cik, companyName);
        scrapingTasks.put(taskId, status);
        
        // 保存新任务到数据库
        saveTaskToDatabase(status);
        
        CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Starting latest 13F scraping task for CIK: " + cik);
                status.setStatus(TaskStatus.RUNNING);
                status.setStartTime(LocalDateTime.now());
                status.setMessage("正在获取最新13F数据...");
                saveTaskToDatabase(status); // 更新运行状态
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
                saveTaskToDatabase(status); // 保存完成状态
                logger.info("Completed latest 13F scraping task for CIK: " + cik);
                
                return 1;
                
            } catch (Exception e) {
                logger.error("Scraping task failed for CIK: " + cik, e);
                status.setStatus(TaskStatus.FAILED);
                status.setEndTime(LocalDateTime.now());
                status.setMessage("爬取失败: " + e.getMessage());
                status.setError(e.getMessage());
                saveTaskToDatabase(status); // 保存失败状态
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
                saveTaskToDatabase(status); // 保存异步失败状态
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
    public ScrapingTask getTaskStatus(String taskId) {
        return scrapingTasks.get(taskId);
    }
    
    /**
     * 获取所有任务状态（从数据库）
     */
    public List<ScrapingTask> getAllTaskStatuses() {
        try {
            // 从数据库获取最新的任务状态
            List<ScrapingTask> dbTasks = taskDAO.getAllTasks();
            // 更新内存中的任务状态
            scrapingTasks.clear();
            for (ScrapingTask task : dbTasks) {
                scrapingTasks.put(task.getTaskId(), task);
            }
            return dbTasks;
        } catch (SQLException e) {
            logger.error("Failed to load tasks from database", e);
            // 如果数据库读取失败，返回内存中的任务
            return new ArrayList<>(scrapingTasks.values());
        }
    }
    
    /**
     * 清理内存中的已完成任务（保留数据库中的所有历史记录）
     */
    public void cleanupCompletedTasks() {
        // 只从内存中移除已完成和失败的任务，保留数据库中的历史记录
        int beforeCount = scrapingTasks.size();
        scrapingTasks.entrySet().removeIf(entry -> {
            ScrapingTask status = entry.getValue();
            return status.getStatus() == TaskStatus.COMPLETED || status.getStatus() == TaskStatus.FAILED;
        });
        int afterCount = scrapingTasks.size();
        logger.info("Cleaned up " + (beforeCount - afterCount) + " completed tasks from memory (database history preserved)");
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
}
