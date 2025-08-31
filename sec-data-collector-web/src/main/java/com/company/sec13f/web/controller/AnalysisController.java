package com.company.sec13f.web.controller;

import com.company.sec13f.repository.mapper.FilingMapper;
import com.company.sec13f.repository.mapper.HoldingMapper;
import com.company.sec13f.repository.entity.Filing;
import com.company.sec13f.repository.entity.Holding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    
    @Autowired
    private FilingMapper filingMapper;
    
    @Autowired
    private HoldingMapper holdingMapper;
    
    public AnalysisController() {
        // Constructor
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
            
            // 使用MyBatis查询机构信息
            List<Filing> filings = filingMapper.selectByCik(cik.trim());
            
            Map<String, Object> overview = new HashMap<>();
            overview.put("cik", cik);
            overview.put("filingCount", filings.size());
            
            if (!filings.isEmpty()) {
                Filing latestFiling = filings.get(0);
                overview.put("companyName", latestFiling.getCompanyName());
                overview.put("latestFilingDate", latestFiling.getFilingDate());
                overview.put("available", true);
            } else {
                overview.put("companyName", "Unknown");
                overview.put("available", false);
                overview.put("message", "No filings found for this CIK");
            }
            
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
            
            // 使用MyBatis查询持仓信息
            List<Filing> filings = filingMapper.selectByCik(cik.trim());
            
            Map<String, Object> response = new HashMap<>();
            response.put("cik", cik);
            response.put("limit", limit);
            
            if (!filings.isEmpty()) {
                // 获取最新的文件的持仓信息
                Long latestFilingId = filings.get(0).getId();
                List<Holding> holdings = holdingMapper.selectByFilingId(latestFilingId);
                
                // 按价值排序并限制数量
                List<Holding> topHoldings = holdings.stream()
                    .sorted((h1, h2) -> {
                        if (h1.getValue() == null && h2.getValue() == null) return 0;
                        if (h1.getValue() == null) return 1;
                        if (h2.getValue() == null) return -1;
                        return h2.getValue().compareTo(h1.getValue());
                    })
                    .limit(limit)
                    .collect(java.util.stream.Collectors.toList());
                    
                response.put("topHoldings", topHoldings);
                response.put("totalHoldings", holdings.size());
            } else {
                response.put("topHoldings", java.util.Arrays.asList());
                response.put("totalHoldings", 0);
                response.put("message", "No filings found for this CIK");
            }
            
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
            
            // 使用MyBatis查询投资组合信息
            List<Filing> filings = filingMapper.selectByCik(cik.trim());
            
            Map<String, Object> summary = new HashMap<>();
            summary.put("cik", cik);
            
            if (!filings.isEmpty()) {
                // 获取最新文件的持仓信息
                Long latestFilingId = filings.get(0).getId();
                List<Holding> holdings = holdingMapper.selectByFilingId(latestFilingId);
                
                // 计算组合总价值
                java.math.BigDecimal totalValue = holdings.stream()
                    .filter(h -> h.getValue() != null)
                    .map(Holding::getValue)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                
                summary.put("totalValue", totalValue);
                summary.put("totalPositions", holdings.size());
                summary.put("latestFilingDate", filings.get(0).getFilingDate());
                summary.put("companyName", filings.get(0).getCompanyName());
            } else {
                summary.put("totalValue", java.math.BigDecimal.ZERO);
                summary.put("totalPositions", 0);
                summary.put("message", "No portfolio data found for this CIK");
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
            
            // 使用MyBatis查询持仓变化信息
            List<Filing> filings = filingMapper.selectByCik(cik.trim());
            
            Map<String, Object> response = new HashMap<>();
            response.put("cik", cik);
            
            if (filings.size() >= 2) {
                // 比较最新两个文件的持仓变化
                Filing currentFiling = filings.get(0);
                Filing previousFiling = filings.get(1);
                
                List<Holding> currentHoldings = holdingMapper.selectByFilingId(currentFiling.getId());
                List<Holding> previousHoldings = holdingMapper.selectByFilingId(previousFiling.getId());
                
                // 简化的变化分析：返回持仓数量变化
                Map<String, Object> change = new HashMap<>();
                change.put("currentPeriod", currentFiling.getFilingDate());
                change.put("previousPeriod", previousFiling.getFilingDate());
                change.put("currentPositions", currentHoldings.size());
                change.put("previousPositions", previousHoldings.size());
                change.put("positionChange", currentHoldings.size() - previousHoldings.size());
                
                response.put("changes", java.util.Arrays.asList(change));
                response.put("available", true);
                
            } else if (filings.size() == 1) {
                response.put("changes", java.util.Arrays.asList());
                response.put("message", "Only one filing found, cannot compare changes");
                response.put("available", false);
            } else {
                response.put("changes", java.util.Arrays.asList());
                response.put("message", "No filings found for this CIK");
                response.put("available", false);
            }
            
            return ResponseEntity.ok(response);
            
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