package com.company.sec13f.repository.test;

import com.company.sec13f.repository.MyBatisSessionFactory;
import com.company.sec13f.repository.entity.Filing;
import com.company.sec13f.repository.entity.Holding;
import com.company.sec13f.repository.entity.ScrapingTask;
import com.company.sec13f.repository.service.FilingRepositoryService;
import com.company.sec13f.repository.service.ScrapingTaskRepositoryService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Repository模块功能测试
 */
public class RepositoryTest {
    
    public static void main(String[] args) {
        System.out.println("🚀 Starting Repository Module Test...");
        
        try {
            // 初始化数据库表结构
            System.out.println("🏗️ Initializing database tables...");
            MyBatisSessionFactory.initializeTables();
            
            // 测试Filing相关功能
            testFilingOperations();
            
            // 测试Task相关功能
            testTaskOperations();
            
            System.out.println("✅ All tests completed successfully!");
            
        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testFilingOperations() {
        System.out.println("\n📄 Testing Filing operations...");
        
        FilingRepositoryService filingService = new FilingRepositoryService();
        
        // 创建测试Filing
        Filing filing = new Filing();
        filing.setCik("0001067983");
        filing.setCompanyName("Berkshire Hathaway Inc");
        filing.setFilingType("13F-HR");
        filing.setFilingDate(LocalDate.now().minusDays(1));
        filing.setAccessionNumber("0000950123-25-008361");
        filing.setFormFile("0000950123-25-008361.txt");
        
        // 创建测试Holdings
        Holding holding1 = new Holding();
        holding1.setNameOfIssuer("Apple Inc");
        holding1.setCusip("037833100");
        holding1.setValue(new BigDecimal("1000000000"));
        holding1.setShares(5000000L);
        
        Holding holding2 = new Holding();
        holding2.setNameOfIssuer("Microsoft Corp");
        holding2.setCusip("594918104");
        holding2.setValue(new BigDecimal("800000000"));
        holding2.setShares(3000000L);
        
        filing.setHoldings(Arrays.asList(holding1, holding2));
        
        // 测试保存
        Long filingId = filingService.saveFiling(filing);
        System.out.println("✅ Filing saved with ID: " + filingId);
        
        // 测试查询
        Filing savedFiling = filingService.getFilingById(filingId);
        System.out.println("✅ Filing retrieved: " + savedFiling.getCompanyName() + 
                          " with " + savedFiling.getHoldings().size() + " holdings");
        
        // 测试按CIK查询
        List<Filing> filings = filingService.getFilingsByCik("0001067983");
        System.out.println("✅ Found " + filings.size() + " filings for CIK");
        
        // 测试统计
        long totalFilings = filingService.countFilings();
        System.out.println("✅ Total filings in database: " + totalFilings);
    }
    
    private static void testTaskOperations() {
        System.out.println("\n📝 Testing Task operations...");
        
        ScrapingTaskRepositoryService taskService = new ScrapingTaskRepositoryService();
        
        // 创建测试Task
        ScrapingTask task = new ScrapingTask();
        task.setTaskId("test_task_" + System.currentTimeMillis());
        task.setCik("0001067983");
        task.setCompanyName("Berkshire Hathaway Inc");
        task.setStatus(ScrapingTask.TaskStatus.PENDING);
        task.setMessage("Test task created");
        
        // 测试保存
        Long taskId = taskService.saveTask(task);
        System.out.println("✅ Task saved with ID: " + taskId);
        
        // 测试更新状态
        task.setStatus(ScrapingTask.TaskStatus.RUNNING);
        task.setStartTime(LocalDateTime.now());
        task.setMessage("Task is running");
        taskService.updateTask(task);
        System.out.println("✅ Task status updated to RUNNING");
        
        // 测试完成任务
        task.setStatus(ScrapingTask.TaskStatus.COMPLETED);
        task.setEndTime(LocalDateTime.now());
        task.setSavedFilings(1);
        task.setMessage("Task completed successfully");
        taskService.updateTask(task);
        System.out.println("✅ Task completed - Duration: " + task.getDurationSeconds() + " seconds");
        
        // 测试查询
        ScrapingTask retrievedTask = taskService.getTaskByTaskId(task.getTaskId());
        System.out.println("✅ Task retrieved: " + retrievedTask.getCompanyName() + 
                          " - Status: " + retrievedTask.getStatus());
        
        // 测试统计
        long totalTasks = taskService.countTasks();
        long completedTasks = taskService.countTasksByStatus(ScrapingTask.TaskStatus.COMPLETED);
        System.out.println("✅ Total tasks: " + totalTasks + ", Completed: " + completedTasks);
        
        // 测试获取最近任务
        List<ScrapingTask> recentTasks = taskService.getRecentTasks(5);
        System.out.println("✅ Recent tasks: " + recentTasks.size());
    }
}