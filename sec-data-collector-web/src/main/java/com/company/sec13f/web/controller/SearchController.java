package com.company.sec13f.web.controller;

import com.company.sec13f.repository.mapper.FilingMapper;
import com.company.sec13f.repository.entity.Filing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    
    @Autowired
    private FilingMapper filingMapper;
    
    public SearchController() {
        // Constructor
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
                // 根据CIK搜索
                filings = filingMapper.selectByCik(cik.trim());
            } else if (companyName != null && !companyName.trim().isEmpty()) {
                // 根据公司名称搜索 - 使用现有方法获取所有公司信息并过滤
                List<Map<String, Object>> companies = filingMapper.selectCompaniesWithHoldings(null, companyName.trim(), "filingDate");
                filings = new java.util.ArrayList<>();
                for (Map<String, Object> company : companies) {
                    if (company.get("cik") != null) {
                        List<Filing> companyFilings = filingMapper.selectByCik(company.get("cik").toString());
                        filings.addAll(companyFilings);
                    }
                }
            } else {
                // 获取所有文件并限制数量
                filings = filingMapper.selectAllWithHoldingsCount();
                if (filings.size() > limit) {
                    filings = filings.subList(0, limit);
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", filings.size());
            response.put("filings", filings);
            
            return ResponseEntity.ok(response);
            
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
            
            // 使用现有的方法搜索公司
            List<Map<String, Object>> companies = filingMapper.selectCompaniesWithHoldings(null, query.trim(), "companyName");
            List<Filing> filings = new java.util.ArrayList<>();
            for (Map<String, Object> company : companies) {
                if (company.get("cik") != null) {
                    List<Filing> companyFilings = filingMapper.selectByCik(company.get("cik").toString());
                    if (!companyFilings.isEmpty()) {
                        filings.add(companyFilings.get(0)); // 只取第一个文件作为代表
                    }
                }
            }
            
            // 提取唯一的公司信息
            Map<String, Map<String, Object>> uniqueCompanies = new HashMap<>();
            for (Filing filing : filings) {
                String cik = filing.getCik();
                if (!uniqueCompanies.containsKey(cik)) {
                    Map<String, Object> company = new HashMap<>();
                    company.put("cik", cik);
                    company.put("companyName", filing.getCompanyName());
                    company.put("filingCount", 1);
                    uniqueCompanies.put(cik, company);
                } else {
                    // 增加文件数量
                    Map<String, Object> company = uniqueCompanies.get(cik);
                    company.put("filingCount", (Integer) company.get("filingCount") + 1);
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", uniqueCompanies.size());
            response.put("companies", uniqueCompanies.values());
            
            return ResponseEntity.ok(response);
            
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