package com.company.sec13f.web.controller;

import com.company.sec13f.repository.database.FilingDAO;
import com.company.sec13f.repository.model.Filing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring MVC Controller for search functionality
 */
@RestController
@RequestMapping("/api/search")
@CrossOrigin(origins = "*")
public class SearchController {
    
    private final FilingDAO filingDAO;
    
    @Autowired
    public SearchController(FilingDAO filingDAO) {
        this.filingDAO = filingDAO;
    }
    
    /**
     * 搜索公司文件
     * GET /api/search/filings?cik=xxx&companyName=xxx
     */
    @GetMapping("/filings")
    public ResponseEntity<?> searchFilings(
            @RequestParam(required = false) String cik,
            @RequestParam(required = false) String companyName,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            List<Filing> filings;
            
            if (cik != null && !cik.trim().isEmpty()) {
                filings = filingDAO.getFilingsByCik(cik.trim());
            } else {
                filings = filingDAO.getAllFilings();
            }
            
            // 限制结果数量
            if (filings.size() > limit) {
                filings = filings.subList(0, limit);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", filings.size());
            response.put("filings", filings);
            
            return ResponseEntity.ok(response);
            
        } catch (SQLException e) {
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Database error: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Search failed: " + e.getMessage()));
        }
    }
    
    /**
     * 搜索公司列表
     * GET /api/search/companies?query=xxx
     */
    @GetMapping("/companies")
    public ResponseEntity<?> searchCompanies(@RequestParam String query) {
        try {
            if (query == null || query.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Query parameter is required"));
            }
            
            // 简化实现：返回所有公司的列表
            List<Filing> allFilings = filingDAO.getAllFilings();
            List<String> companies = allFilings.stream()
                .map(Filing::getCompanyName)
                .filter(name -> name != null && name.toLowerCase().contains(query.toLowerCase()))
                .distinct()
                .limit(20)
                .collect(java.util.stream.Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", companies.size());
            response.put("companies", companies);
            
            return ResponseEntity.ok(response);
            
        } catch (SQLException e) {
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Database error: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("Search failed: " + e.getMessage()));
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