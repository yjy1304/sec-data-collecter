package com.company.sec13f.web.controller;

import com.company.sec13f.repository.entity.Company;
import com.company.sec13f.web.service.CompanyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 公司维护管理控制器 - Spring MVC版本
 */
@RestController
@RequestMapping("/api/companies")
public class CompanyController {
    
    private static final Logger logger = LoggerFactory.getLogger(CompanyController.class);
    
    @Autowired
    private CompanyService companyService;
    
    /**
     * 获取公司列表（分页）
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getCompanies(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "companyName") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortOrder) {
        
        try {
            Map<String, Object> result = companyService.getCompanies(page, size, keyword, isActive, sortBy, sortOrder);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("处理获取公司列表请求失败", e);
            return ResponseEntity.status(500).body(createErrorResponse("获取公司列表失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取统计信息
     */
    @GetMapping(value = "/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getCompanyStats() {
        try {
            Map<String, Object> stats = companyService.getCompanyStats();
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("处理获取统计信息请求失败", e);
            return ResponseEntity.status(500).body(createErrorResponse("获取统计信息失败: " + e.getMessage()));
        }
    }
    
    /**
     * 根据ID获取公司信息
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Company> getCompany(@PathVariable Long id) {
        try {
            Company company = companyService.getCompanyById(id);
            
            if (company != null) {
                return ResponseEntity.ok(company);
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            logger.error("处理获取公司信息请求失败, ID: " + id, e);
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * 添加公司
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> addCompany(@RequestBody Company company) {
        try {
            Company savedCompany = companyService.addCompany(company);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "公司添加成功");
            response.put("id", savedCompany.getId());
            response.put("company", savedCompany);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("添加公司参数错误: " + e.getMessage());
            return ResponseEntity.status(409).body(createErrorResponse(e.getMessage()));
            
        } catch (Exception e) {
            logger.error("处理添加公司请求失败", e);
            return ResponseEntity.status(500).body(createErrorResponse("添加公司失败: " + e.getMessage()));
        }
    }
    
    /**
     * 更新公司
     */
    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> updateCompany(@PathVariable Long id, @RequestBody Company company) {
        try {
            Company updatedCompany = companyService.updateCompany(id, company);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "公司更新成功");
            response.put("company", updatedCompany);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("不存在")) {
                return ResponseEntity.notFound().build();
            } else {
                logger.warn("更新公司参数错误: " + e.getMessage());
                return ResponseEntity.status(409).body(createErrorResponse(e.getMessage()));
            }
            
        } catch (Exception e) {
            logger.error("处理更新公司请求失败, ID: " + id, e);
            return ResponseEntity.status(500).body(createErrorResponse("更新公司失败: " + e.getMessage()));
        }
    }
    
    /**
     * 删除公司
     */
    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> deleteCompany(@PathVariable Long id) {
        try {
            boolean deleted = companyService.deleteCompany(id);
            
            if (deleted) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "公司删除成功");
                
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(500).body(createErrorResponse("删除公司失败"));
            }
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            logger.error("处理删除公司请求失败, ID: " + id, e);
            return ResponseEntity.status(500).body(createErrorResponse("删除公司失败: " + e.getMessage()));
        }
    }
    
    /**
     * 导出公司列表为CSV
     */
    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<String> exportCompanies(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive) {
        
        try {
            List<Company> companies = companyService.getCompaniesForExport(keyword, isActive);
            
            StringBuilder csv = new StringBuilder();
            csv.append("CIK,公司名称,显示名称,行业,部门,网站,描述,状态,总文件数,最新文件日期,创建时间,更新时间\n");
            
            for (Company company : companies) {
                csv.append(formatCsvField(company.getCik())).append(",");
                csv.append(formatCsvField(company.getCompanyName())).append(",");
                csv.append(formatCsvField(company.getDisplayName())).append(",");
                csv.append(formatCsvField(company.getIndustry())).append(",");
                csv.append(formatCsvField(company.getSector())).append(",");
                csv.append(formatCsvField(company.getWebsite())).append(",");
                csv.append(formatCsvField(company.getDescription())).append(",");
                csv.append(formatCsvField(company.getIsActive() != null ? (company.getIsActive() ? "活跃" : "非活跃") : "")).append(",");
                csv.append(company.getTotalFilings() != null ? company.getTotalFilings() : 0).append(",");
                csv.append(company.getLastFilingDate() != null ? company.getLastFilingDate().toString() : "").append(",");
                csv.append(company.getCreatedAt() != null ? company.getCreatedAt().toString() : "").append(",");
                csv.append(company.getUpdatedAt() != null ? company.getUpdatedAt().toString() : "").append("\n");
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=\"companies.csv\"");
            headers.add("Content-Type", "text/csv; charset=UTF-8");
            
            logger.info("📊 导出公司列表: {} 条记录", companies.size());
            return ResponseEntity.ok().headers(headers).body(csv.toString());
            
        } catch (Exception e) {
            logger.error("处理导出公司列表请求失败", e);
            return ResponseEntity.status(500).body("Error generating CSV: " + e.getMessage());
        }
    }
    
    /**
     * 批量添加公司
     */
    @PostMapping(value = "/batch", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> batchAddCompanies(@RequestBody List<Company> companies) {
        try {
            int addedCount = companyService.batchAddCompanies(companies);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "批量添加公司成功");
            response.put("addedCount", addedCount);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("批量添加公司参数错误: " + e.getMessage());
            return ResponseEntity.status(409).body(createErrorResponse(e.getMessage()));
            
        } catch (Exception e) {
            logger.error("处理批量添加公司请求失败", e);
            return ResponseEntity.status(500).body(createErrorResponse("批量添加公司失败: " + e.getMessage()));
        }
    }
    
    /**
     * 根据行业获取公司列表
     */
    @GetMapping(value = "/industry/{industry}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Company>> getCompaniesByIndustry(@PathVariable String industry) {
        try {
            List<Company> companies = companyService.getCompaniesByIndustry(industry);
            return ResponseEntity.ok(companies);
            
        } catch (Exception e) {
            logger.error("处理根据行业获取公司请求失败, 行业: " + industry, e);
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * 根据部门获取公司列表
     */
    @GetMapping(value = "/sector/{sector}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Company>> getCompaniesBySector(@PathVariable String sector) {
        try {
            List<Company> companies = companyService.getCompaniesBySector(sector);
            return ResponseEntity.ok(companies);
            
        } catch (Exception e) {
            logger.error("处理根据部门获取公司请求失败, 部门: " + sector, e);
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * 获取行业统计信息
     */
    @GetMapping(value = "/stats/industry", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getIndustryStats() {
        try {
            List<Map<String, Object>> stats = companyService.getIndustryStats();
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("处理获取行业统计请求失败", e);
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * 获取部门统计信息
     */
    @GetMapping(value = "/stats/sector", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getSectorStats() {
        try {
            List<Map<String, Object>> stats = companyService.getSectorStats();
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("处理获取部门统计请求失败", e);
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * 根据CIK查询公司（用于API集成）
     */
    @GetMapping(value = "/cik/{cik}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Company> getCompanyByCik(@PathVariable String cik) {
        try {
            Company company = companyService.getCompanyByCik(cik);
            
            if (company != null) {
                return ResponseEntity.ok(company);
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            logger.error("处理根据CIK获取公司请求失败, CIK: " + cik, e);
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * 创建错误响应
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", message);
        return error;
    }
    
    /**
     * 格式化CSV字段（处理特殊字符和空值）
     */
    private String formatCsvField(String value) {
        if (value == null) {
            return "";
        }
        
        // 如果包含逗号、双引号或换行符，需要用双引号包围，并转义双引号
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        
        return value;
    }
}