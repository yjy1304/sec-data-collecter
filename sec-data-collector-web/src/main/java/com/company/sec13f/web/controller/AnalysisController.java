package com.company.sec13f.web.controller;

import com.company.sec13f.repository.database.FilingDAO;
import com.company.sec13f.service.HoldingAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring MVC Controller for portfolio analysis APIs
 */
@RestController
@RequestMapping("/api/analysis")
@CrossOrigin(origins = "*")
public class AnalysisController {
    
    private final HoldingAnalysisService analysisService;
    
    @Autowired
    public AnalysisController(HoldingAnalysisService analysisService) {
        this.analysisService = analysisService;
    }
    
    /**
     * 获取机构概览信息
     * GET /api/analysis/overview?cik=0001524258
     */
    @GetMapping("/overview")
    public ResponseEntity<?> getInstitutionOverview(@RequestParam String cik) {
        try {
            if (cik == null || cik.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("CIK parameter is required"));
            }
            
            // 简化实现：返回基本的机构信息
            Map<String, Object> overview = new HashMap<>();
            overview.put("cik", cik);
            overview.put("message", "Institution overview data");
            overview.put("available", true);
            
            return ResponseEntity.ok(overview);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Internal server error: " + e.getMessage()));
        }
    }
    
    /**
     * 获取机构的热门持仓
     * GET /api/analysis/top-holdings?cik=0001524258&limit=20
     */
    @GetMapping("/top-holdings")
    public ResponseEntity<?> getTopHoldings(
            @RequestParam String cik,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            if (cik == null || cik.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("CIK parameter is required"));
            }
            
            // 简化实现：返回基本的持仓信息
            Map<String, Object> response = new HashMap<>();
            response.put("cik", cik);
            response.put("limit", limit);
            response.put("topHoldings", java.util.Arrays.asList());
            response.put("message", "Top holdings data");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Internal server error: " + e.getMessage()));
        }
    }
    
    /**
     * 获取投资组合摘要
     * GET /api/analysis/portfolio-summary?cik=0001524258
     */
    @GetMapping("/portfolio-summary")
    public ResponseEntity<?> getPortfolioSummary(@RequestParam String cik) {
        try {
            if (cik == null || cik.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("CIK parameter is required"));
            }
            
            HoldingAnalysisService.PortfolioSummary summary = analysisService.getPortfolioSummary(cik);
            if (summary == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(summary);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Internal server error: " + e.getMessage()));
        }
    }
    
    /**
     * 获取持仓变化分析
     * GET /api/analysis/holding-changes?cik=0001524258
     */
    @GetMapping("/holding-changes")
    public ResponseEntity<?> getHoldingChanges(@RequestParam String cik) {
        try {
            if (cik == null || cik.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("CIK parameter is required"));
            }
            
            List<HoldingAnalysisService.HoldingChange> changes = analysisService.getHoldingChanges(cik);
            return ResponseEntity.ok(changes);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Internal server error: " + e.getMessage()));
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