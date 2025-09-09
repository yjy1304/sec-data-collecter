package com.company.sec13f.service.plugin;

import com.company.sec13f.repository.entity.Task;
import com.company.sec13f.repository.enums.TaskType;
import com.company.sec13f.repository.mapper.FilingMapper;
import com.company.sec13f.repository.mapper.HoldingMapper;
import com.company.sec13f.service.scraper.RealSECScraper;
import com.company.sec13f.service.util.DataValidator;
import com.company.sec13f.service.util.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 数据抓取任务处理插件
 * 负责处理SCRAP_HOLDING类型的任务
 */
@Component
public class ScrapingTaskProcessPlugin implements TaskProcessPlugin {
    
    private final RealSECScraper scraper;
    private final FilingMapper filingMapper;
    private final HoldingMapper holdingMapper;
    private final Logger logger;
    
    @Autowired
    public ScrapingTaskProcessPlugin(FilingMapper filingMapper, HoldingMapper holdingMapper) {
        this.scraper = new RealSECScraper();
        this.filingMapper = filingMapper;
        this.holdingMapper = holdingMapper;
        this.logger = Logger.getInstance();
    }
    
    @Override
    public TaskResult handleTask(Task task) {
        try {
            // 从任务参数中解析CIK和公司名称
            TaskParameters params = new TaskParameters(task.getTaskParameters());
            String cik = params.getString("cik");
            String companyName = params.getString("companyName");
            
            if (cik == null || companyName == null) {
                return TaskResult.failure("任务参数不完整：缺少CIK或公司名称");
            }
            
            logger.scrapingStarted(cik, companyName);
            
            // 使用新的搜索API获取公司的13F文件列表
            List<com.company.sec13f.repository.model.Filing> filings = scraper.getCompanyFilingsWithSearchAPI(cik);
            logger.info("Found " + filings.size() + " 13F filings for " + companyName);
            
            int savedCount = 0;
            for (com.company.sec13f.repository.model.Filing filing : filings) {
                // 检查是否已经存在
                if (!isFilingExists(filing.getAccessionNumber())) {
                    // 新的搜索API已经包含了持仓数据，直接使用
                    filing.setCompanyName(companyName);
                    filing.setCik(cik);
                    
                    // 验证数据
                    DataValidator.ValidationResult validation = DataValidator.validateFiling(filing);
                    if (validation.isValid()) {
                        // 转换为Entity对象并保存到数据库
                        com.company.sec13f.repository.entity.Filing entityFiling = convertToEntity(filing);
                        int inserted = filingMapper.insert(entityFiling);
                        if (inserted > 0) {
                            // 如果Filing有持仓数据，也保存持仓信息
                            if (filing.getHoldings() != null && !filing.getHoldings().isEmpty()) {
                                for (com.company.sec13f.repository.model.Holding modelHolding : filing.getHoldings()) {
                                    com.company.sec13f.repository.entity.Holding entityHolding = convertToEntity(modelHolding);
                                    entityHolding.setFilingId(entityFiling.getId());
                                    holdingMapper.insert(entityHolding);
                                }
                            }
                            savedCount++;
                        }
                    } else {
                        logger.warn("文件 " + filing.getAccessionNumber() + " 验证失败: " + 
                            validation.getAllErrors().get(0));
                    }
                }
                
                // 添加延迟以遵守API频率限制
                Thread.sleep(100);
            }
            
            String resultMessage = "成功爬取并保存了 " + savedCount + " 个新的13F文件";
            logger.info(resultMessage);
            return TaskResult.success(resultMessage);
            
        } catch (java.io.IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("timeout")) {
                String errorMessage = "网络请求超时: " + e.getMessage();
                logger.warn("⏰ 抓取任务网络超时，将重试: " + errorMessage);
                return TaskResult.failure(errorMessage, e);
            } else if (e.getMessage() != null && e.getMessage().contains("Connection")) {
                String errorMessage = "网络连接失败: " + e.getMessage();
                logger.warn("🔌 抓取任务连接失败，将重试: " + errorMessage);
                return TaskResult.failure(errorMessage, e);
            } else {
                String errorMessage = "网络IO异常: " + e.getMessage();
                logger.error("🌐 抓取任务网络异常", e);
                return TaskResult.failure(errorMessage, e);
            }
        } catch (InterruptedException e) {
            String errorMessage = "抓取任务被中断: " + e.getMessage();
            logger.warn("🛑 抓取任务被中断: " + errorMessage);
            Thread.currentThread().interrupt(); // 重置中断状态
            return TaskResult.failure(errorMessage, e);
        } catch (Exception e) {
            String errorMessage = "数据抓取失败: " + e.getMessage();
            logger.error("💥 抓取任务未知异常", e);
            return TaskResult.failure(errorMessage, e);
        }
    }
    
    @Override
    public TaskType getTaskType() {
        return TaskType.SCRAP_HOLDING;
    }
    
    
    /**
     * 检查文件是否已存在
     */
    private boolean isFilingExists(String accessionNumber) {
        // 使用MyBatis FilingMapper检查文件是否存在
        boolean exists = filingMapper.existsByAccessionNumber(accessionNumber);
        logger.debug("检查文件存在性: " + accessionNumber + " -> " + exists);
        return exists;
    }
    
    /**
     * 将model.Filing转换为entity.Filing
     */
    private com.company.sec13f.repository.entity.Filing convertToEntity(com.company.sec13f.repository.model.Filing modelFiling) {
        com.company.sec13f.repository.entity.Filing entityFiling = new com.company.sec13f.repository.entity.Filing();
        entityFiling.setCik(modelFiling.getCik());
        entityFiling.setCompanyName(modelFiling.getCompanyName());
        entityFiling.setFilingType(modelFiling.getFilingType());
        entityFiling.setFilingDate(modelFiling.getFilingDate());
        entityFiling.setAccessionNumber(modelFiling.getAccessionNumber());
        entityFiling.setFormFile(modelFiling.getFormFile());
        entityFiling.setCreatedAt(java.time.LocalDateTime.now());
        entityFiling.setUpdatedAt(java.time.LocalDateTime.now());
        return entityFiling;
    }
    
    /**
     * 将model.Holding转换为entity.Holding
     */
    private com.company.sec13f.repository.entity.Holding convertToEntity(com.company.sec13f.repository.model.Holding modelHolding) {
        com.company.sec13f.repository.entity.Holding entityHolding = new com.company.sec13f.repository.entity.Holding();
        entityHolding.setNameOfIssuer(modelHolding.getNameOfIssuer());
        entityHolding.setCusip(modelHolding.getCusip());
        entityHolding.setValue(modelHolding.getValue());
        entityHolding.setShares(modelHolding.getShares());
        entityHolding.setCik(modelHolding.getCik());
        entityHolding.setCompanyName(modelHolding.getCompanyName());
        entityHolding.setCreatedAt(java.time.LocalDateTime.now());
        entityHolding.setUpdatedAt(java.time.LocalDateTime.now());
        return entityHolding;
    }
}