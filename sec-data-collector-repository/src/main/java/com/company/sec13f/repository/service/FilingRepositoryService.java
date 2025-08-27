package com.company.sec13f.repository.service;

import com.company.sec13f.repository.MyBatisSessionFactory;
import com.company.sec13f.repository.entity.Filing;
import com.company.sec13f.repository.entity.Holding;
import com.company.sec13f.repository.mapper.FilingMapper;
import com.company.sec13f.repository.mapper.HoldingMapper;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Filing数据仓储服务
 * 提供Filing相关的高级数据访问操作，包括事务管理
 */
public class FilingRepositoryService {
    
    private static final Logger logger = LoggerFactory.getLogger(FilingRepositoryService.class);
    
    /**
     * 保存Filing及其关联的Holdings（事务操作）
     * @param filing Filing实体（包含Holdings列表）
     * @return 保存的Filing的ID
     */
    public Long saveFiling(Filing filing) {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            try {
                FilingMapper filingMapper = session.getMapper(FilingMapper.class);
                HoldingMapper holdingMapper = session.getMapper(HoldingMapper.class);
                
                // 保存Filing
                filingMapper.insert(filing);
                Long filingId = filing.getId();
                
                logger.info("📄 Filing saved with ID: {} - CIK: {}, Accession: {}", 
                    filingId, filing.getCik(), filing.getAccessionNumber());
                
                // 保存Holdings
                if (filing.getHoldings() != null && !filing.getHoldings().isEmpty()) {
                    // 设置每个Holding的filing_id
                    for (Holding holding : filing.getHoldings()) {
                        holding.setFilingId(filingId);
                    }
                    
                    // 批量插入Holdings
                    holdingMapper.batchInsert(filing.getHoldings());
                    
                    logger.info("💼 {} holdings saved for filing ID: {}", 
                        filing.getHoldings().size(), filingId);
                } else {
                    logger.warn("⚠️ No holdings found in filing to save");
                }
                
                session.commit();
                logger.info("🎯 Transaction committed successfully - Filing ID: {}", filingId);
                
                return filingId;
                
            } catch (Exception e) {
                session.rollback();
                logger.error("❌ Failed to save filing, transaction rolled back", e);
                throw new RuntimeException("Failed to save filing: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * 根据ID查询Filing（包含Holdings）
     */
    public Filing getFilingById(Long id) {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            FilingMapper filingMapper = session.getMapper(FilingMapper.class);
            HoldingMapper holdingMapper = session.getMapper(HoldingMapper.class);
            
            Filing filing = filingMapper.selectById(id);
            if (filing != null) {
                filing.setHoldings(holdingMapper.selectByFilingId(id));
            }
            
            return filing;
        }
    }
    
    /**
     * 根据CIK查询所有Filing
     */
    public List<Filing> getFilingsByCik(String cik) {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            FilingMapper filingMapper = session.getMapper(FilingMapper.class);
            return filingMapper.selectByCik(cik);
        }
    }
    
    /**
     * 根据Accession Number查询Filing
     */
    public Filing getFilingByAccessionNumber(String accessionNumber) {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            FilingMapper filingMapper = session.getMapper(FilingMapper.class);
            return filingMapper.selectByAccessionNumber(accessionNumber);
        }
    }
    
    /**
     * 根据Accession Number和Form File查询Filing
     */
    public Filing getFilingByAccessionAndFormFile(String accessionNumber, String formFile) {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            FilingMapper filingMapper = session.getMapper(FilingMapper.class);
            return filingMapper.selectByAccessionAndFormFile(accessionNumber, formFile);
        }
    }
    
    /**
     * 获取所有Filing（带Holdings数量统计）
     */
    public List<Filing> getAllFilingsWithHoldingsCount() {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            FilingMapper filingMapper = session.getMapper(FilingMapper.class);
            return filingMapper.selectAllWithHoldingsCount();
        }
    }
    
    /**
     * 检查Filing是否存在
     */
    public boolean filingExists(String accessionNumber) {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            FilingMapper filingMapper = session.getMapper(FilingMapper.class);
            return filingMapper.existsByAccessionNumber(accessionNumber);
        }
    }
    
    /**
     * 删除Filing及其所有关联的Holdings（级联删除）
     */
    public boolean deleteFiling(Long id) {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            try {
                FilingMapper filingMapper = session.getMapper(FilingMapper.class);
                HoldingMapper holdingMapper = session.getMapper(HoldingMapper.class);
                
                // 先删除关联的Holdings
                int holdingsDeleted = holdingMapper.deleteByFilingId(id);
                
                // 再删除Filing
                int filingDeleted = filingMapper.deleteById(id);
                
                session.commit();
                
                logger.info("🗑️ Deleted filing ID: {} with {} holdings", id, holdingsDeleted);
                return filingDeleted > 0;
                
            } catch (Exception e) {
                session.rollback();
                logger.error("❌ Failed to delete filing ID: {}", id, e);
                throw new RuntimeException("Failed to delete filing: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * 统计Filing总数
     */
    public long countFilings() {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            FilingMapper filingMapper = session.getMapper(FilingMapper.class);
            return filingMapper.countAll();
        }
    }
    
    /**
     * 根据CIK统计Filing数量
     */
    public long countFilingsByCik(String cik) {
        try (SqlSession session = MyBatisSessionFactory.openSession()) {
            FilingMapper filingMapper = session.getMapper(FilingMapper.class);
            return filingMapper.countByCik(cik);
        }
    }
}