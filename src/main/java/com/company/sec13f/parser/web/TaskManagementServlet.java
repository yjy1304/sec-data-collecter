package com.company.sec13f.parser.web;

import com.company.sec13f.parser.database.FilingDAO;
import com.company.sec13f.parser.model.Filing;
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
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/tasks/*")
public class TaskManagementServlet extends HttpServlet {
    
    private DataScrapingService scrapingService;
    private FilingDAO filingDAO;
    private ObjectMapper objectMapper;
    
    @Override
    public void init() throws ServletException {
        super.init();
        this.filingDAO = new FilingDAO();
        this.scrapingService = new DataScrapingService(filingDAO);
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String pathInfo = request.getPathInfo();
        
        if (pathInfo == null || pathInfo.equals("/") || pathInfo.equals("/index.html")) {
            // 显示任务管理页面
            showTaskManagementPage(request, response);
        } else if (pathInfo.equals("/api/tasks")) {
            // 返回任务状态API
            handleTasksAPI(request, response);
        } else if (pathInfo.equals("/api/filings")) {
            // 返回已保存的文件API
            handleFilingsAPI(request, response);
        } else if (pathInfo.startsWith("/api/filing/")) {
            // 返回具体文件详情
            handleFilingDetailAPI(request, response, pathInfo);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String pathInfo = request.getPathInfo();
        
        if (pathInfo != null && pathInfo.equals("/api/cleanup")) {
            handleCleanupAPI(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
    
    /**
     * 显示任务管理页面
     */
    private void showTaskManagementPage(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        out.println("<!DOCTYPE html>");
        out.println("<html lang='zh-CN'>");
        out.println("<head>");
        out.println("    <meta charset='UTF-8'>");
        out.println("    <meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        out.println("    <title>13F数据拉取任务管理</title>");
        out.println("    <style>");
        out.println("        body { font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }");
        out.println("        .container { max-width: 1200px; margin: 0 auto; }");
        out.println("        .header { background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); margin-bottom: 20px; }");
        out.println("        .section { background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); margin-bottom: 20px; }");
        out.println("        .stats { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; margin-bottom: 20px; }");
        out.println("        .stat-card { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 15px; border-radius: 8px; text-align: center; }");
        out.println("        .stat-card h3 { margin: 0 0 10px 0; font-size: 24px; }");
        out.println("        .stat-card p { margin: 0; opacity: 0.9; }");
        out.println("        table { width: 100%; border-collapse: collapse; margin-top: 15px; }");
        out.println("        th, td { padding: 12px; text-align: left; border-bottom: 1px solid #ddd; }");
        out.println("        th { background-color: #f8f9fa; font-weight: bold; }");
        out.println("        .status-running { color: #007bff; font-weight: bold; }");
        out.println("        .status-completed { color: #28a745; font-weight: bold; }");
        out.println("        .status-failed { color: #dc3545; font-weight: bold; }");
        out.println("        .status-pending { color: #ffc107; font-weight: bold; }");
        out.println("        .btn { padding: 8px 16px; border: none; border-radius: 4px; cursor: pointer; text-decoration: none; display: inline-block; }");
        out.println("        .btn-primary { background: #007bff; color: white; }");
        out.println("        .btn-success { background: #28a745; color: white; }");
        out.println("        .btn-danger { background: #dc3545; color: white; }");
        out.println("        .btn:hover { opacity: 0.8; }");
        out.println("        .loading { text-align: center; padding: 20px; color: #666; }");
        out.println("        .holdings-preview { max-height: 150px; overflow-y: auto; border: 1px solid #ddd; padding: 10px; border-radius: 4px; background: #f9f9f9; }");
        out.println("        .holding-item { margin-bottom: 8px; padding: 8px; background: white; border-radius: 4px; border-left: 3px solid #007bff; }");
        out.println("    </style>");
        out.println("</head>");
        out.println("<body>");
        out.println("    <div class='container'>");
        out.println("        <div class='header'>");
        out.println("            <h1>📊 SEC 13F数据拉取任务管理</h1>");
        out.println("            <p>管理和监控13F文件的数据拉取任务状态，查看已保存的持仓数据</p>");
        out.println("        </div>");
        
        out.println("        <div class='stats' id='stats'>");
        out.println("            <div class='stat-card'>");
        out.println("                <h3 id='totalTasks'>-</h3>");
        out.println("                <p>总任务数</p>");
        out.println("            </div>");
        out.println("            <div class='stat-card'>");
        out.println("                <h3 id='runningTasks'>-</h3>");
        out.println("                <p>运行中</p>");
        out.println("            </div>");
        out.println("            <div class='stat-card'>");
        out.println("                <h3 id='completedTasks'>-</h3>");
        out.println("                <p>已完成</p>");
        out.println("            </div>");
        out.println("            <div class='stat-card'>");
        out.println("                <h3 id='savedFilings'>-</h3>");
        out.println("                <p>已保存文件</p>");
        out.println("            </div>");
        out.println("        </div>");
        
        out.println("        <div class='section'>");
        out.println("            <h2>🔄 拉取任务状态</h2>");
        out.println("            <button class='btn btn-success' onclick='refreshTasks()'>刷新状态</button>");
        out.println("            <button class='btn btn-danger' onclick='cleanupTasks()'>清理已完成任务</button>");
        out.println("            <div id='tasksTable'>加载中...</div>");
        out.println("        </div>");
        
        out.println("        <div class='section'>");
        out.println("            <h2>📁 已保存的13F文件</h2>");
        out.println("            <button class='btn btn-primary' onclick='refreshFilings()'>刷新文件列表</button>");
        out.println("            <div id='filingsTable'>加载中...</div>");
        out.println("        </div>");
        
        out.println("    </div>");
        
        out.println("    <script>");
        out.println("        let tasksInterval;");
        out.println("        ");
        out.println("        function refreshTasks() {");
        out.println("            fetch('/tasks/api/tasks')");
        out.println("                .then(response => response.json())");
        out.println("                .then(data => {");
        out.println("                    updateStats(data);");
        out.println("                    updateTasksTable(data.tasks);");
        out.println("                })");
        out.println("                .catch(error => {");
        out.println("                    document.getElementById('tasksTable').innerHTML = '<p class=\"loading\">加载任务状态失败: ' + error.message + '</p>';");
        out.println("                });");
        out.println("        }");
        out.println("        ");
        out.println("        function refreshFilings() {");
        out.println("            fetch('/tasks/api/filings')");
        out.println("                .then(response => response.json())");
        out.println("                .then(data => {");
        out.println("                    updateFilingsTable(data.filings);");
        out.println("                    document.getElementById('savedFilings').textContent = data.totalFilings;");
        out.println("                })");
        out.println("                .catch(error => {");
        out.println("                    document.getElementById('filingsTable').innerHTML = '<p class=\"loading\">加载文件列表失败: ' + error.message + '</p>';");
        out.println("                });");
        out.println("        }");
        out.println("        ");
        out.println("        function cleanupTasks() {");
        out.println("            if (confirm('确定要清理所有已完成和失败的任务吗？')) {");
        out.println("                fetch('/tasks/api/cleanup', { method: 'POST' })");
        out.println("                    .then(response => response.json())");
        out.println("                    .then(() => {");
        out.println("                        alert('任务清理完成');");
        out.println("                        refreshTasks();");
        out.println("                    })");
        out.println("                    .catch(error => alert('清理失败: ' + error.message));");
        out.println("            }");
        out.println("        }");
        out.println("        ");
        out.println("        function updateStats(data) {");
        out.println("            document.getElementById('totalTasks').textContent = data.totalTasks;");
        out.println("            document.getElementById('runningTasks').textContent = data.runningTasks;");
        out.println("            document.getElementById('completedTasks').textContent = data.completedTasks;");
        out.println("        }");
        out.println("        ");
        out.println("        function updateTasksTable(tasks) {");
        out.println("            if (!tasks || tasks.length === 0) {");
        out.println("                document.getElementById('tasksTable').innerHTML = '<p class=\"loading\">暂无拉取任务</p>';");
        out.println("                return;");
        out.println("            }");
        out.println("            ");
        out.println("            let html = '<table><thead><tr><th>任务ID</th><th>公司</th><th>CIK</th><th>状态</th><th>消息</th><th>持续时间</th><th>已保存文件数</th></tr></thead><tbody>';");
        out.println("            ");
        out.println("            tasks.forEach(task => {");
        out.println("                const statusClass = 'status-' + task.status.toLowerCase();");
        out.println("                const duration = task.durationSeconds ? task.durationSeconds + 's' : '-';");
        out.println("                html += `<tr>");
        out.println("                    <td>${task.taskId}</td>");
        out.println("                    <td>${task.companyName}</td>");
        out.println("                    <td>${task.cik}</td>");
        out.println("                    <td><span class=\"${statusClass}\">${task.status}</span></td>");
        out.println("                    <td>${task.message || ''}</td>");
        out.println("                    <td>${duration}</td>");
        out.println("                    <td>${task.savedFilings || 0}</td>");
        out.println("                </tr>`;");
        out.println("            });");
        out.println("            ");
        out.println("            html += '</tbody></table>';");
        out.println("            document.getElementById('tasksTable').innerHTML = html;");
        out.println("        }");
        out.println("        ");
        out.println("        function updateFilingsTable(filings) {");
        out.println("            if (!filings || filings.length === 0) {");
        out.println("                document.getElementById('filingsTable').innerHTML = '<p class=\"loading\">暂无已保存的文件</p>';");
        out.println("                return;");
        out.println("            }");
        out.println("            ");
        out.println("            let html = '<table><thead><tr><th>公司名称</th><th>CIK</th><th>Accession Number</th><th>文件日期</th><th>持仓数量</th><th>操作</th></tr></thead><tbody>';");
        out.println("            ");
        out.println("            filings.forEach(filing => {");
        out.println("                const filingDate = filing.filingDate || '-';");
        out.println("                const holdingsCount = filing.holdingsCount || 0;");
        out.println("                html += `<tr>");
        out.println("                    <td>${filing.companyName || '未知'}</td>");
        out.println("                    <td>${filing.cik}</td>");
        out.println("                    <td>${filing.accessionNumber}</td>");
        out.println("                    <td>${filingDate}</td>");
        out.println("                    <td>${holdingsCount}</td>");
        out.println("                    <td><button class=\"btn btn-primary\" onclick=\"viewFiling('${filing.accessionNumber}')\">查看详情</button></td>");
        out.println("                </tr>`;");
        out.println("            });");
        out.println("            ");
        out.println("            html += '</tbody></table>';");
        out.println("            document.getElementById('filingsTable').innerHTML = html;");
        out.println("        }");
        out.println("        ");
        out.println("        function viewFiling(accessionNumber) {");
        out.println("            window.open('/tasks/api/filing/' + accessionNumber, '_blank');");
        out.println("        }");
        out.println("        ");
        out.println("        // 自动刷新运行中的任务");
        out.println("        function startAutoRefresh() {");
        out.println("            tasksInterval = setInterval(() => {");
        out.println("                refreshTasks();");
        out.println("            }, 5000); // 每5秒刷新一次");
        out.println("        }");
        out.println("        ");
        out.println("        // 页面加载时初始化");
        out.println("        document.addEventListener('DOMContentLoaded', () => {");
        out.println("            refreshTasks();");
        out.println("            refreshFilings();");
        out.println("            startAutoRefresh();");
        out.println("        });");
        out.println("        ");
        out.println("        // 页面卸载时清理定时器");
        out.println("        window.addEventListener('beforeunload', () => {");
        out.println("            if (tasksInterval) clearInterval(tasksInterval);");
        out.println("        });");
        out.println("    </script>");
        out.println("</body>");
        out.println("</html>");
    }
    
    /**
     * 处理任务状态API
     */
    private void handleTasksAPI(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");
        
        PrintWriter out = response.getWriter();
        
        try {
            List<DataScrapingService.ScrapingStatus> allTasks = scrapingService.getAllTaskStatuses();
            
            long runningTasks = allTasks.stream()
                    .filter(task -> task.getStatus() == DataScrapingService.TaskStatus.RUNNING)
                    .count();
            
            long completedTasks = allTasks.stream()
                    .filter(task -> task.getStatus() == DataScrapingService.TaskStatus.COMPLETED)
                    .count();
            
            long failedTasks = allTasks.stream()
                    .filter(task -> task.getStatus() == DataScrapingService.TaskStatus.FAILED)
                    .count();
            
            Map<String, Object> result = new HashMap<>();
            result.put("tasks", allTasks);
            result.put("totalTasks", allTasks.size());
            result.put("runningTasks", runningTasks);
            result.put("completedTasks", completedTasks);
            result.put("failedTasks", failedTasks);
            
            objectMapper.writeValue(out, result);
            
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            Map<String, Object> error = new HashMap<>();
            error.put("error", true);
            error.put("message", e.getMessage());
            objectMapper.writeValue(out, error);
        }
    }
    
    /**
     * 处理文件列表API
     */
    private void handleFilingsAPI(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");
        
        PrintWriter out = response.getWriter();
        
        try {
            List<Filing> filings = filingDAO.getAllFilings();
            
            Map<String, Object> result = new HashMap<>();
            result.put("filings", filings);
            result.put("totalFilings", filings.size());
            
            objectMapper.writeValue(out, result);
            
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            Map<String, Object> error = new HashMap<>();
            error.put("error", true);
            error.put("message", e.getMessage());
            objectMapper.writeValue(out, error);
        }
    }
    
    /**
     * 处理文件详情API
     */
    private void handleFilingDetailAPI(HttpServletRequest request, HttpServletResponse response, String pathInfo) 
            throws IOException {
        
        response.setContentType("text/html;charset=UTF-8");
        
        String accessionNumber = pathInfo.substring("/api/filing/".length());
        PrintWriter out = response.getWriter();
        
        try {
            Long filingId = filingDAO.getFilingIdByAccessionNumber(accessionNumber);
            if (filingId == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Filing not found: " + accessionNumber);
                return;
            }
            
            List<Filing> filings = filingDAO.getFilingsByCik(""); // 这里需要改进，暂时用空字符串
            Filing targetFiling = null;
            for (Filing f : filings) {
                if (accessionNumber.equals(f.getAccessionNumber())) {
                    targetFiling = f;
                    break;
                }
            }
            
            if (targetFiling == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Filing details not found");
                return;
            }
            
            // 生成文件详情页面
            out.println("<!DOCTYPE html>");
            out.println("<html lang='zh-CN'>");
            out.println("<head>");
            out.println("    <meta charset='UTF-8'>");
            out.println("    <title>13F文件详情 - " + accessionNumber + "</title>");
            out.println("    <style>");
            out.println("        body { font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }");
            out.println("        .container { max-width: 1000px; margin: 0 auto; background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }");
            out.println("        .header { border-bottom: 2px solid #007bff; padding-bottom: 15px; margin-bottom: 20px; }");
            out.println("        .info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 30px; }");
            out.println("        .info-item { padding: 15px; background: #f8f9fa; border-radius: 6px; }");
            out.println("        .holdings-table { width: 100%; border-collapse: collapse; margin-top: 20px; }");
            out.println("        .holdings-table th, .holdings-table td { padding: 12px; text-align: left; border-bottom: 1px solid #ddd; }");
            out.println("        .holdings-table th { background-color: #007bff; color: white; }");
            out.println("        .holdings-table tr:nth-child(even) { background-color: #f2f2f2; }");
            out.println("        .btn { padding: 10px 20px; background: #007bff; color: white; text-decoration: none; border-radius: 4px; display: inline-block; margin: 10px 10px 10px 0; }");
            out.println("        .btn:hover { background: #0056b3; }");
            out.println("        .total-value { font-size: 18px; font-weight: bold; color: #28a745; }");
            out.println("    </style>");
            out.println("</head>");
            out.println("<body>");
            out.println("    <div class='container'>");
            out.println("        <div class='header'>");
            out.println("            <h1>📄 13F文件详情</h1>");
            out.println("            <p>Accession Number: <strong>" + accessionNumber + "</strong></p>");
            out.println("        </div>");
            
            out.println("        <div class='info-grid'>");
            out.println("            <div class='info-item'>");
            out.println("                <strong>公司名称:</strong><br>" + (targetFiling.getCompanyName() != null ? targetFiling.getCompanyName() : "未知"));
            out.println("            </div>");
            out.println("            <div class='info-item'>");
            out.println("                <strong>CIK:</strong><br>" + targetFiling.getCik());
            out.println("            </div>");
            out.println("            <div class='info-item'>");
            out.println("                <strong>文件类型:</strong><br>" + targetFiling.getFilingType());
            out.println("            </div>");
            out.println("            <div class='info-item'>");
            out.println("                <strong>文件日期:</strong><br>" + (targetFiling.getFilingDate() != null ? targetFiling.getFilingDate().toString() : "未知"));
            out.println("            </div>");
            out.println("        </div>");
            
            if (targetFiling.getHoldings() != null && !targetFiling.getHoldings().isEmpty()) {
                out.println("        <h2>📊 持仓信息 (" + targetFiling.getHoldings().size() + " 个持仓)</h2>");
                out.println("        <table class='holdings-table'>");
                out.println("            <thead>");
                out.println("                <tr>");
                out.println("                    <th>发行人名称</th>");
                out.println("                    <th>CUSIP</th>");
                out.println("                    <th>市值 (美元)</th>");
                out.println("                    <th>持有股数</th>");
                out.println("                </tr>");
                out.println("            </thead>");
                out.println("            <tbody>");
                
                java.math.BigDecimal totalValue = java.math.BigDecimal.ZERO;
                for (com.company.sec13f.parser.model.Holding holding : targetFiling.getHoldings()) {
                    out.println("                <tr>");
                    out.println("                    <td>" + (holding.getNameOfIssuer() != null ? holding.getNameOfIssuer() : "-") + "</td>");
                    out.println("                    <td>" + (holding.getCusip() != null ? holding.getCusip() : "-") + "</td>");
                    out.println("                    <td>$" + (holding.getValue() != null ? String.format("%,d", holding.getValue().longValue()) : "0") + "</td>");
                    out.println("                    <td>" + (holding.getShares() != null ? String.format("%,d", holding.getShares()) : "0") + "</td>");
                    out.println("                </tr>");
                    
                    if (holding.getValue() != null) {
                        totalValue = totalValue.add(holding.getValue());
                    }
                }
                
                out.println("            </tbody>");
                out.println("        </table>");
                out.println("        <div class='total-value'>总市值: $" + String.format("%,d", totalValue.longValue()) + "</div>");
            } else {
                out.println("        <p>该文件没有持仓信息。</p>");
            }
            
            out.println("        <a href='/tasks/' class='btn'>返回任务管理</a>");
            out.println("        <a href='javascript:window.close()' class='btn'>关闭窗口</a>");
            out.println("    </div>");
            out.println("</body>");
            out.println("</html>");
            
        } catch (SQLException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }
    
    /**
     * 处理清理任务API
     */
    private void handleCleanupAPI(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");
        
        PrintWriter out = response.getWriter();
        
        try {
            scrapingService.cleanupCompletedTasks();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "任务清理完成");
            
            objectMapper.writeValue(out, result);
            
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            Map<String, Object> error = new HashMap<>();
            error.put("error", true);
            error.put("message", e.getMessage());
            objectMapper.writeValue(out, error);
        }
    }
    
    @Override
    public void destroy() {
        if (scrapingService != null) {
            scrapingService.shutdown();
        }
        super.destroy();
    }
}