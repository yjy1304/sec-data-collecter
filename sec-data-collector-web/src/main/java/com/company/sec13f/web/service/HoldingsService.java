package com.company.sec13f.web.service;

import com.company.sec13f.repository.MyBatisSessionFactory;
import com.company.sec13f.repository.entity.Filing;
import com.company.sec13f.repository.entity.Holding;
import com.company.sec13f.repository.mapper.FilingMapper;
import com.company.sec13f.repository.mapper.HoldingMapper;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 持仓信息服务类
 */
@Service
public class HoldingsService {
    
    private static final Logger logger = LoggerFactory.getLogger(HoldingsService.class);
    
    /**
     * 获取所有有数据的公司列表
     */
    public List<Map<String, Object>> getCompaniesWithHoldings(String cik, String name, String sortBy) {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            FilingMapper filingMapper = session.getMapper(FilingMapper.class);
            
            // 使用自定义查询方法
            return filingMapper.selectCompaniesWithHoldings(cik, name, sortBy);
        } catch (Exception e) {
            logger.error("Error getting companies with holdings", e);
            throw new RuntimeException("Failed to get companies with holdings: " + e.getMessage(), e);
        }
    }
    
    /**
     * 根据CIK获取公司的所有持仓信息
     */
    public Map<String, Object> getCompanyHoldings(String cik, Double minValue, String search, String sortBy, String sortOrder) {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            FilingMapper filingMapper = session.getMapper(FilingMapper.class);
            HoldingMapper holdingMapper = session.getMapper(HoldingMapper.class);
            
            Map<String, Object> result = new HashMap<>();
            
            // 获取公司基本信息
            Filing latestFiling = filingMapper.selectLatestByCik(cik);
            if (latestFiling == null) {
                return null;
            }
            
            result.put("cik", cik);
            result.put("companyName", latestFiling.getCompanyName());
            result.put("latestFilingDate", latestFiling.getFilingDate());
            
            // 获取持仓数据
            List<Holding> holdings = holdingMapper.selectByCikWithFilingFiltered(cik, minValue, search, sortBy, sortOrder);
            
            // 计算统计信息
            Map<String, Object> summary = new HashMap<>();
            summary.put("totalHoldings", holdings.size());
            
            double totalValue = holdings.stream()
                    .mapToDouble(h -> h.getValue() != null ? h.getValue().doubleValue() : 0.0)
                    .sum();
            summary.put("totalValue", totalValue);
            summary.put("avgPosition", holdings.size() > 0 ? totalValue / holdings.size() : 0);
            
            result.put("summary", summary);
            result.put("holdings", holdings);
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error getting company holdings for CIK: " + cik, e);
            throw new RuntimeException("Failed to get company holdings: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取持仓统计信息
     */
    public Map<String, Object> getHoldingsStats() {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            FilingMapper filingMapper = session.getMapper(FilingMapper.class);
            HoldingMapper holdingMapper = session.getMapper(HoldingMapper.class);
            
            Map<String, Object> stats = new HashMap<>();
            
            // 获取基本统计
            long totalCompanies = filingMapper.countDistinctCompanies();
            long totalFilings = filingMapper.countAll();
            long totalHoldings = holdingMapper.countAll();
            BigDecimal totalMarketValue = holdingMapper.sumAllValues();
            
            stats.put("totalCompanies", totalCompanies);
            stats.put("totalFilings", totalFilings);
            stats.put("totalHoldings", totalHoldings);
            stats.put("totalMarketValue", totalMarketValue != null ? totalMarketValue.doubleValue() : 0.0);
            stats.put("avgHoldingValue", totalHoldings > 0 && totalMarketValue != null ? 
                totalMarketValue.doubleValue() / totalHoldings : 0.0);
            
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
     * 获取公司持仓数据用于CSV导出
     */
    public List<Holding> getHoldingsForExport(String cik) {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            HoldingMapper holdingMapper = session.getMapper(HoldingMapper.class);
            
            return holdingMapper.selectByCikWithFilingForExport(cik);
            
        } catch (Exception e) {
            logger.error("Error getting holdings for export, CIK: " + cik, e);
            throw new RuntimeException("Failed to get holdings for export: " + e.getMessage(), e);
        }
    }
}