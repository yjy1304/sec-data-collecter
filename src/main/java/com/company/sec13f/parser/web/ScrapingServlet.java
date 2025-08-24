package com.company.sec13f.parser.web;

import com.company.sec13f.parser.database.FilingDAO;
import com.company.sec13f.parser.service.DataScrapingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/scraping/*")
public class ScrapingServlet extends HttpServlet {
    
    private DataScrapingService scrapingService;
    private ObjectMapper objectMapper;
    
    @Override
    public void init() throws ServletException {
        super.init();
        FilingDAO filingDAO = new FilingDAO();
        this.scrapingService = new DataScrapingService(filingDAO);
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");
        
        String pathInfo = request.getPathInfo();
        PrintWriter out = response.getWriter();
        
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                sendError(response, 400, "Scraping endpoint required");
                return;
            }
            
            String[] pathParts = pathInfo.split("/");
            if (pathParts.length < 2) {
                sendError(response, 400, "Invalid scraping endpoint");
                return;
            }
            
            String endpoint = pathParts[1];
            
            switch (endpoint) {
                case "status":
                    handleTaskStatus(request, response, out);
                    break;
                case "tasks":
                    handleAllTasks(response, out);
                    break;
                case "cleanup":
                    handleCleanup(response, out);
                    break;
                default:
                    sendError(response, 404, "Scraping endpoint not found: " + endpoint);
            }
            
        } catch (Exception e) {
            sendError(response, 500, "Internal server error: " + e.getMessage());
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");
        
        String pathInfo = request.getPathInfo();
        PrintWriter out = response.getWriter();
        
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                sendError(response, 400, "Scraping endpoint required");
                return;
            }
            
            String[] pathParts = pathInfo.split("/");
            if (pathParts.length < 2) {
                sendError(response, 400, "Invalid scraping endpoint");
                return;
            }
            
            String endpoint = pathParts[1];
            
            switch (endpoint) {
                case "scrape":
                    handleScrapeRequest(request, response, out);
                    break;
                case "scrape-latest":
                    handleScrapeLatestRequest(request, response, out);
                    break;
                case "scrape-batch":
                    handleBatchScrapeRequest(request, response, out);
                    break;
                default:
                    sendError(response, 404, "Scraping endpoint not found: " + endpoint);
            }
            
        } catch (Exception e) {
            sendError(response, 500, "Internal server error: " + e.getMessage());
        }
    }
    
    /**
     * 处理爬取请求
     */
    private void handleScrapeRequest(HttpServletRequest request, HttpServletResponse response, PrintWriter out) 
            throws IOException {
        
        String cik = request.getParameter("cik");
        String companyName = request.getParameter("companyName");
        
        if (cik == null || cik.trim().isEmpty()) {
            sendError(response, 400, "CIK parameter is required");
            return;
        }
        
        if (companyName == null || companyName.trim().isEmpty()) {
            companyName = "Unknown Company";
        }
        
        String taskId = scrapingService.scrapeCompanyData(cik.trim(), companyName.trim());
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("taskId", taskId);
        result.put("message", "Scraping task started for CIK: " + cik);
        
        objectMapper.writeValue(out, result);
    }
    
    /**
     * 处理爬取最新文件请求
     */
    private void handleScrapeLatestRequest(HttpServletRequest request, HttpServletResponse response, PrintWriter out) 
            throws IOException {
        
        String cik = request.getParameter("cik");
        String companyName = request.getParameter("companyName");
        
        if (cik == null || cik.trim().isEmpty()) {
            sendError(response, 400, "CIK parameter is required");
            return;
        }
        
        if (companyName == null || companyName.trim().isEmpty()) {
            companyName = "Unknown Company";
        }
        
        String taskId = scrapingService.scrapeLatest13F(cik.trim(), companyName.trim());
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("taskId", taskId);
        result.put("message", "Latest 13F scraping task started for CIK: " + cik);
        
        objectMapper.writeValue(out, result);
    }
    
    /**
     * 处理批量爬取请求
     */
    private void handleBatchScrapeRequest(HttpServletRequest request, HttpServletResponse response, PrintWriter out) 
            throws IOException {
        
        // 示例批量爬取一些知名机构
        Map<String, String> companies = new HashMap<>();
        companies.put("0001524258", "Alibaba Group Holding Limited");
        companies.put("0001067983", "Berkshire Hathaway Inc");
        companies.put("0001013594", "BlackRock Inc");
        companies.put("0000909832", "Vanguard Group Inc");
        companies.put("0001364742", "State Street Corp");
        
        List<String> taskIds = scrapingService.scrapeMultipleCompanies(companies);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("taskIds", taskIds);
        result.put("message", "Started " + taskIds.size() + " scraping tasks");
        result.put("companies", companies);
        
        objectMapper.writeValue(out, result);
    }
    
    /**
     * 处理任务状态查询
     */
    private void handleTaskStatus(HttpServletRequest request, HttpServletResponse response, PrintWriter out) 
            throws IOException {
        
        String taskId = request.getParameter("taskId");
        
        if (taskId == null || taskId.trim().isEmpty()) {
            sendError(response, 400, "TaskId parameter is required");
            return;
        }
        
        DataScrapingService.ScrapingStatus status = scrapingService.getTaskStatus(taskId);
        
        if (status == null) {
            sendError(response, 404, "Task not found: " + taskId);
            return;
        }
        
        objectMapper.writeValue(out, status);
    }
    
    /**
     * 处理获取所有任务状态
     */
    private void handleAllTasks(HttpServletResponse response, PrintWriter out) throws IOException {
        List<DataScrapingService.ScrapingStatus> allTasks = scrapingService.getAllTaskStatuses();
        
        Map<String, Object> result = new HashMap<>();
        result.put("tasks", allTasks);
        result.put("totalTasks", allTasks.size());
        
        long runningTasks = allTasks.stream()
                .filter(task -> task.getStatus() == DataScrapingService.TaskStatus.RUNNING)
                .count();
        result.put("runningTasks", runningTasks);
        
        long completedTasks = allTasks.stream()
                .filter(task -> task.getStatus() == DataScrapingService.TaskStatus.COMPLETED)
                .count();
        result.put("completedTasks", completedTasks);
        
        long failedTasks = allTasks.stream()
                .filter(task -> task.getStatus() == DataScrapingService.TaskStatus.FAILED)
                .count();
        result.put("failedTasks", failedTasks);
        
        objectMapper.writeValue(out, result);
    }
    
    /**
     * 处理清理完成任务
     */
    private void handleCleanup(HttpServletResponse response, PrintWriter out) throws IOException {
        scrapingService.cleanupCompletedTasks();
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Completed tasks cleaned up");
        
        objectMapper.writeValue(out, result);
    }
    
    private void sendError(HttpServletResponse response, int statusCode, String message) 
            throws IOException {
        response.setStatus(statusCode);
        PrintWriter out = response.getWriter();
        
        Map<String, Object> error = new HashMap<>();
        error.put("error", true);
        error.put("message", message);
        error.put("status", statusCode);
        
        objectMapper.writeValue(out, error);
    }
    
    @Override
    public void destroy() {
        if (scrapingService != null) {
            scrapingService.shutdown();
        }
        super.destroy();
    }
}