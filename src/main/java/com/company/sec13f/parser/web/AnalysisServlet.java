package com.company.sec13f.parser.web;

import com.company.sec13f.parser.database.FilingDAO;
import com.company.sec13f.parser.service.HoldingAnalysisService;
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

@WebServlet("/api/analysis/*")
public class AnalysisServlet extends HttpServlet {
    
    private HoldingAnalysisService analysisService;
    private ObjectMapper objectMapper;
    
    @Override
    public void init() throws ServletException {
        super.init();
        FilingDAO filingDAO = new FilingDAO();
        this.analysisService = new HoldingAnalysisService(filingDAO);
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
                sendError(response, 400, "Analysis endpoint required");
                return;
            }
            
            String[] pathParts = pathInfo.split("/");
            if (pathParts.length < 2) {
                sendError(response, 400, "Invalid analysis endpoint");
                return;
            }
            
            String endpoint = pathParts[1];
            String cik = request.getParameter("cik");
            
            if (cik == null || cik.trim().isEmpty()) {
                sendError(response, 400, "CIK parameter is required");
                return;
            }
            
            switch (endpoint) {
                case "overview":
                    handleOverview(cik, response, out);
                    break;
                case "top-holdings":
                    handleTopHoldings(cik, request, response, out);
                    break;
                case "portfolio-summary":
                    handlePortfolioSummary(cik, response, out);
                    break;
                case "holding-changes":
                    handleHoldingChanges(cik, response, out);
                    break;
                case "holding-trends":
                    handleHoldingTrends(cik, request, response, out);
                    break;
                default:
                    sendError(response, 404, "Analysis endpoint not found: " + endpoint);
            }
            
        } catch (SQLException e) {
            sendError(response, 500, "Database error: " + e.getMessage());
        } catch (Exception e) {
            sendError(response, 500, "Internal server error: " + e.getMessage());
        }
    }
    
    private void handleOverview(String cik, HttpServletResponse response, PrintWriter out) 
            throws SQLException, IOException {
        
        HoldingAnalysisService.InstitutionAnalysis analysis = analysisService.analyzeInstitution(cik);
        HoldingAnalysisService.PortfolioSummary summary = analysisService.getPortfolioSummary(cik);
        List<HoldingAnalysisService.TopHolding> topHoldings = analysisService.getTopHoldingsByValue(cik, 10);
        
        Map<String, Object> overview = new HashMap<>();
        Map<String, Object> institution = new HashMap<>();
        institution.put("cik", analysis.getCik());
        institution.put("name", analysis.getInstitutionName());
        institution.put("totalFilings", analysis.getFilings().size());
        overview.put("institution", institution);
        
        if (summary != null) {
            overview.put("portfolioSummary", summary);
        }
        
        overview.put("topHoldings", topHoldings);
        
        objectMapper.writeValue(out, overview);
    }
    
    private void handleTopHoldings(String cik, HttpServletRequest request, 
                                 HttpServletResponse response, PrintWriter out) 
            throws SQLException, IOException {
        
        int limit = 20;
        String limitParam = request.getParameter("limit");
        if (limitParam != null) {
            try {
                limit = Integer.parseInt(limitParam);
                limit = Math.max(1, Math.min(100, limit)); // Limit between 1 and 100
            } catch (NumberFormatException e) {
                // Use default limit
            }
        }
        
        List<HoldingAnalysisService.TopHolding> topHoldings = analysisService.getTopHoldingsByValue(cik, limit);
        
        Map<String, Object> result = new HashMap<>();
        result.put("topHoldings", topHoldings);
        result.put("totalCount", topHoldings.size());
        
        objectMapper.writeValue(out, result);
    }
    
    private void handlePortfolioSummary(String cik, HttpServletResponse response, PrintWriter out) 
            throws SQLException, IOException {
        
        HoldingAnalysisService.PortfolioSummary summary = analysisService.getPortfolioSummary(cik);
        
        if (summary == null) {
            sendError(response, 404, "No portfolio data found for CIK: " + cik);
            return;
        }
        
        objectMapper.writeValue(out, summary);
    }
    
    private void handleHoldingChanges(String cik, HttpServletResponse response, PrintWriter out) 
            throws SQLException, IOException {
        
        List<HoldingAnalysisService.HoldingChange> changes = analysisService.getHoldingChanges(cik);
        
        Map<String, Object> result = new HashMap<>();
        result.put("changes", changes);
        result.put("totalCount", changes.size());
        
        objectMapper.writeValue(out, result);
    }
    
    private void handleHoldingTrends(String cik, HttpServletRequest request, 
                                   HttpServletResponse response, PrintWriter out) 
            throws SQLException, IOException {
        
        String cusip = request.getParameter("cusip");
        if (cusip == null || cusip.trim().isEmpty()) {
            sendError(response, 400, "CUSIP parameter is required for trends analysis");
            return;
        }
        
        List<HoldingAnalysisService.HoldingTrend> trends = analysisService.getHoldingTrends(cik, cusip);
        
        Map<String, Object> result = new HashMap<>();
        result.put("cusip", cusip);
        result.put("trends", trends);
        result.put("totalDataPoints", trends.size());
        
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
}