package com.company.sec13f.service.plugin;

import com.company.sec13f.repository.database.FilingDAO;
import com.company.sec13f.repository.entity.Task;
import com.company.sec13f.repository.enums.TaskType;
import com.company.sec13f.repository.model.Filing;
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
    private final FilingDAO filingDAO;
    private final Logger logger;
    
    @Autowired
    public ScrapingTaskProcessPlugin(FilingDAO filingDAO) {
        this.scraper = new RealSECScraper();
        this.filingDAO = filingDAO;
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
            
            // 获取公司的13F文件列表
            List<Filing> filings = scraper.getCompanyFilings(cik);
            logger.info("Found " + filings.size() + " 13F filings for " + companyName);
            
            int savedCount = 0;
            for (Filing filing : filings) {
                // 检查是否已经存在
                if (!isFilingExists(filing.getAccessionNumber())) {
                    // 获取详细的13F数据
                    Filing detailedFiling = scraper.get13FDetails(filing.getAccessionNumber(), cik);
                    detailedFiling.setCompanyName(companyName);
                    detailedFiling.setCik(cik);
                    
                    // 验证数据
                    DataValidator.ValidationResult validation = DataValidator.validateFiling(detailedFiling);
                    if (validation.isValid()) {
                        // 保存到数据库
                        filingDAO.saveFiling(detailedFiling);
                        savedCount++;
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
            
        } catch (Exception e) {
            String errorMessage = "数据抓取失败: " + e.getMessage();
            logger.error("Scraping task failed", e);
            return TaskResult.failure(errorMessage, e);
        }
    }
    
    @Override
    public TaskType getTaskType() {
        return TaskType.SCRAP_HOLDING;
    }
    
    
    /**
     * 检查文件是否已存在
     * 临时实现：总是返回false以确保系统能正常运行
     * TODO: 实现真正的文件存在性检查
     */
    private boolean isFilingExists(String accessionNumber) {
        // 临时实现，总是返回false
        // 实际应该查询数据库检查是否存在此accessionNumber
        logger.debug("检查文件存在性: " + accessionNumber + " (临时返回false)");
        return false;
    }
}