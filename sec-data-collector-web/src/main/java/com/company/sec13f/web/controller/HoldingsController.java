package com.company.sec13f.web.controller;

import com.company.sec13f.web.service.HoldingsService;
import com.company.sec13f.repository.entity.Holding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 持仓信息查看控制器
 */
@RestController
@RequestMapping("/api/holdings")
public class HoldingsController {
    
    @Autowired
    private HoldingsService holdingsService;
    
    /**
     * 获取所有有数据的公司列表
     */
    @GetMapping(value = "/companies", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getCompanies(
            @RequestParam(required = false) String cik,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "name") String sortBy) {
        
        try {
            List<Map<String, Object>> companies = holdingsService.getCompaniesWithHoldings(cik, name, sortBy);
            return ResponseEntity.ok(companies);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Collections.emptyList());
        }
    }
    
    /**
     * 根据CIK获取公司的所有持仓信息
     */
    @GetMapping(value = "/company/{cik}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getCompanyHoldings(
            @PathVariable String cik,
            @RequestParam(required = false) Double minValue,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "value") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(required = false) String filingDateFrom,
            @RequestParam(required = false) String filingDateTo,
            @RequestParam(required = false) String reportPeriodFrom,
            @RequestParam(required = false) String reportPeriodTo) {
        
        try {
            Map<String, Object> result = holdingsService.getCompanyHoldings(cik, minValue, search, sortBy, sortOrder, 
                    filingDateFrom, filingDateTo, reportPeriodFrom, reportPeriodTo);
            if (result == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Collections.emptyMap());
        }
    }
    
    /**
     * 获取持仓统计信息
     */
    @GetMapping(value = "/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getHoldingsStats() {
        try {
            Map<String, Object> stats = holdingsService.getHoldingsStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Collections.emptyMap());
        }
    }
    
    /**
     * 导出持仓数据为CSV格式
     */
    @GetMapping(value = "/export/{cik}/csv", produces = "text/csv")
    public ResponseEntity<String> exportHoldingsCSV(@PathVariable String cik) {
        
        try {
            List<Holding> holdings = holdingsService.getHoldingsForExport(cik);
            
            StringBuilder csv = new StringBuilder();
            csv.append("股票名称,CUSIP,市值(千美元),股票数量,报告日期,报告编号\n");
            
            for (Holding holding : holdings) {
                csv.append("\"").append(holding.getNameOfIssuer()).append("\",");
                csv.append(holding.getCusip()).append(",");
                csv.append(holding.getValue()).append(",");
                csv.append(holding.getShares()).append(",");
                csv.append(holding.getFiling().getFilingDate()).append(",");
                csv.append(holding.getFiling().getAccessionNumber()).append("\n");
            }
            
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"holdings_" + cik + ".csv\"")
                    .body(csv.toString());
                    
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error generating CSV");
        }
    }
}