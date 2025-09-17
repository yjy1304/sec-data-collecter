package com.company.sec13f.web.service;

import com.company.sec13f.repository.entity.Filing;
import com.company.sec13f.repository.entity.Holding;
import com.company.sec13f.repository.entity.MergeHolding;
import com.company.sec13f.repository.mapper.FilingMapper;
import com.company.sec13f.repository.mapper.HoldingMapper;
import com.company.sec13f.repository.mapper.MergeHoldingMapper;
import com.company.sec13f.repository.param.MergeHoldingsQueryParam;
import org.apache.commons.compress.utils.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * 持仓信息服务类
 */
@Service
public class HoldingsService {
    
    private static final Logger logger = LoggerFactory.getLogger(HoldingsService.class);
    
    @Autowired
    private FilingMapper filingMapper;
    
    @Autowired
    private MergeHoldingMapper mergeHoldingMapper;
    
    /**
     * 获取所有有数据的公司列表
     */
    public List<Map<String, Object>> getCompaniesWithHoldings(String cik, String name, String sortBy) {
        try {
            // 使用自定义查询方法
            return filingMapper.selectCompaniesWithHoldings(cik, name, sortBy);
        } catch (Exception e) {
            logger.error("Error getting companies with holdings", e);
            throw new RuntimeException("Failed to get companies with holdings: " + e.getMessage(), e);
        }
    }
    
    /**
     * 根据CIK获取公司的所有持仓信息（使用合并后的持仓数据）
     */
    public Map<String, Object> getCompanyHoldings(String cik, Double minValue, String search, String sortBy, String sortOrder, String filingDateFrom, String filingDateTo, String reportPeriodFrom, String reportPeriodTo) {
        try {
            
            Map<String, Object> result = new HashMap<>();
            
            // 获取公司基本信息
            Filing latestFiling = filingMapper.selectLatestByCik(cik);
            if (latestFiling == null) {
                return null;
            }
            
            // 获取最新的报告期间
            String latestReportPeriod = filingMapper.selectLatestReportPeriodByCik(cik);
            
            result.put("cik", cik);
            result.put("companyName", latestFiling.getCompanyName());
            result.put("latestFilingDate", latestFiling.getFilingDate());
            result.put("latestReportPeriod", latestReportPeriod);
            
            // 创建查询参数对象并执行SQL查询（包含所有筛选条件）
            MergeHoldingsQueryParam queryParam = new MergeHoldingsQueryParam(
                cik, minValue, search, sortBy, sortOrder, reportPeriodFrom, reportPeriodTo
            );
            
            List<MergeHolding> filteredHoldings = mergeHoldingMapper.selectByQueryParam(queryParam);
            
            // 如果没有查询到任何结果，检查是否该CIK存在
            if (filteredHoldings.isEmpty()) {
                List<Filing> companyFilings = filingMapper.selectByCik(cik);
                if (companyFilings.isEmpty()) {
                    result.put("summary", createEmptySummary());
                    result.put("holdings", Lists.newArrayList());
                    return result;
                }
            }
            
            // 计算统计信息
            Map<String, Object> summary = new HashMap<>();
            summary.put("totalHoldings", filteredHoldings.size());
            
            double totalValue = filteredHoldings.stream()
                    .mapToDouble(h -> h.getValue() != null ? h.getValue().doubleValue() : 0.0)
                    .sum();
            summary.put("totalValue", totalValue);
            summary.put("avgPosition", filteredHoldings.size() > 0 ? totalValue / filteredHoldings.size() : 0);
            
            result.put("summary", summary);
            result.put("holdings", filteredHoldings);
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error getting company holdings for CIK: " + cik, e);
            throw new RuntimeException("Failed to get company holdings: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取持仓统计信息（使用合并后的持仓数据）
     */
    public Map<String, Object> getHoldingsStats() {
        try {
            
            Map<String, Object> stats = new HashMap<>();
            
            // 获取基本统计
            long totalCompanies = filingMapper.countDistinctCompanies();
            long totalFilings = filingMapper.countAll();
            long totalMergeHoldings = mergeHoldingMapper.countAll();
            
            // 计算总市值 - 遍历所有merge_holdings记录
            List<Filing> allFilings = filingMapper.selectAllWithHoldingsCount();
            double totalMarketValue = 0.0;
            for (Filing filing : allFilings) {
                List<MergeHolding> filingHoldings = mergeHoldingMapper.selectByFilingId(filing.getId());
                for (MergeHolding holding : filingHoldings) {
                    if (holding.getValue() != null) {
                        totalMarketValue += holding.getValue().doubleValue();
                    }
                }
            }
            
            stats.put("totalCompanies", totalCompanies);
            stats.put("totalFilings", totalFilings);
            stats.put("totalHoldings", totalMergeHoldings);
            stats.put("totalMarketValue", totalMarketValue);
            stats.put("avgHoldingValue", totalMergeHoldings > 0 ? totalMarketValue / totalMergeHoldings : 0.0);
            
            // 获取最新的报告日期
            Filing latestFiling = filingMapper.selectLatest();
            if (latestFiling != null) {
                stats.put("latestFilingDate", latestFiling.getFilingDate());
            }
            
            return stats;
            
        } catch (Exception e) {
            logger.error("Error getting holdings stats", e);
            throw new RuntimeException("Failed to get holdings stats: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取所有报告期间（去重并倒序排列）
     */
    public List<String> getDistinctReportPeriods() {
        try {
            
            return filingMapper.selectDistinctReportPeriods();
            
        } catch (Exception e) {
            logger.error("Error getting distinct report periods", e);
            throw new RuntimeException("Failed to get distinct report periods: " + e.getMessage(), e);
        }
    }
    
    /**
     * 根据CIK获取该公司的所有报告期间（去重并倒序排列）
     */
    public List<String> getDistinctReportPeriodsByCik(String cik) {
        try {
            
            return filingMapper.selectDistinctReportPeriodsByCik(cik);
            
        } catch (Exception e) {
            logger.error("Error getting distinct report periods for CIK: " + cik, e);
            throw new RuntimeException("Failed to get distinct report periods for CIK: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取公司持仓数据用于CSV导出（使用合并后的持仓数据）
     */
    public List<Holding> getHoldingsForExport(String cik) {
        try {
            
            // 获取该公司所有filing
            List<Filing> companyFilings = filingMapper.selectByCik(cik);
            List<Holding> exportHoldings = new ArrayList<>();
            
            // 将MergeHolding转换为Holding格式用于导出
            for (Filing filing : companyFilings) {
                List<MergeHolding> mergeHoldings = mergeHoldingMapper.selectByFilingId(filing.getId());
                for (MergeHolding mergeHolding : mergeHoldings) {
                    Holding holding = new Holding();
                    holding.setNameOfIssuer(mergeHolding.getNameOfIssuer());
                    holding.setCusip(mergeHolding.getCusip());
                    holding.setValue(mergeHolding.getValue());
                    holding.setShares(mergeHolding.getShares());
                    holding.setCik(mergeHolding.getCik());
                    holding.setCompanyName(mergeHolding.getCompanyName());
                    holding.setFiling(filing); // 设置filing信息用于导出
                    exportHoldings.add(holding);
                }
            }
            
            // 按市值倒序排序
            exportHoldings.sort((h1, h2) -> {
                BigDecimal v1 = h1.getValue() != null ? h1.getValue() : BigDecimal.ZERO;
                BigDecimal v2 = h2.getValue() != null ? h2.getValue() : BigDecimal.ZERO;
                return v2.compareTo(v1);
            });
            
            return exportHoldings;
            
        } catch (Exception e) {
            logger.error("Error getting holdings for export, CIK: " + cik, e);
            throw new RuntimeException("Failed to get holdings for export: " + e.getMessage(), e);
        }
    }
    
    /**
     * 创建空的统计信息
     */
    private Map<String, Object> createEmptySummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalHoldings", 0);
        summary.put("totalValue", 0.0);
        summary.put("avgPosition", 0.0);
        return summary;
    }
    
}