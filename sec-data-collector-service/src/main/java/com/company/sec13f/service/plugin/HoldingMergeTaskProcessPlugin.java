package com.company.sec13f.service.plugin;

import com.company.sec13f.repository.entity.MergeHolding;
import com.company.sec13f.repository.entity.Task;
import com.company.sec13f.repository.enums.TaskType;
import com.company.sec13f.repository.mapper.MergeHoldingMapper;
import com.company.sec13f.service.util.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 持仓合并任务处理插件
 * 负责处理HOLDING_MERGE类型的任务
 * 将同一filing_id下相同cusip的持仓记录进行合并，累加市值和股票数
 */
@Component
public class HoldingMergeTaskProcessPlugin implements TaskProcessPlugin {
    
    private final MergeHoldingMapper mergeHoldingMapper;
    private final Logger logger;
    
    @Autowired
    public HoldingMergeTaskProcessPlugin(MergeHoldingMapper mergeHoldingMapper) {
        this.mergeHoldingMapper = mergeHoldingMapper;
        this.logger = Logger.getInstance();
    }
    
    @Override
    public TaskResult handleTask(Task task) {
        try {
            // 从任务参数中解析filing_id
            TaskParameters params = new TaskParameters(task.getTaskParameters());
            Long filingId = params.getLong("filingId");
            String cik = params.getString("cik");
            String companyName = params.getString("companyName");
            
            if (filingId == null) {
                return TaskResult.failure("任务参数不完整：缺少filingId");
            }
            
            logger.info(String.format("🔄 开始执行持仓合并任务 - FilingId: %d, CIK: %s, 公司: %s", filingId, cik, companyName));
            
            // 1. 删除该filing_id的现有合并记录（如果存在）
            long deletedCount = mergeHoldingMapper.deleteByFilingId(filingId);
            if (deletedCount > 0) {
                logger.info(String.format("🗑️ 删除现有合并记录: %d 条", deletedCount));
            }
            
            // 2. 查询并聚合该filing_id下的持仓数据
            List<Map<String, Object>> aggregatedData = mergeHoldingMapper.selectAggregatedHoldingsByFilingId(filingId);
            
            if (aggregatedData.isEmpty()) {
                return TaskResult.failure("未找到filing_id=" + filingId + "的持仓数据");
            }
            
            logger.info(String.format("📊 查询到 %d 个聚合后的持仓记录", aggregatedData.size()));
            
            // 3. 转换为MergeHolding实体并批量插入
            List<MergeHolding> mergeHoldings = new ArrayList<>();
            
            for (Map<String, Object> data : aggregatedData) {
                MergeHolding mergeHolding = new MergeHolding();
                mergeHolding.setHoldingId(getLongValue(data, "first_holding_id"));
                mergeHolding.setFilingId(getLongValue(data, "filing_id"));
                mergeHolding.setCik(getStringValue(data, "cik"));
                mergeHolding.setCompanyName(getStringValue(data, "company_name"));
                mergeHolding.setReportPeriod(getStringValue(data, "report_period"));
                mergeHolding.setNameOfIssuer(getStringValue(data, "name_of_issuer"));
                mergeHolding.setCusip(getStringValue(data, "cusip"));
                mergeHolding.setValue(getBigDecimalValue(data, "total_value"));
                mergeHolding.setShares(getLongValue(data, "total_shares"));
                
                mergeHoldings.add(mergeHolding);
            }
            
            // 4. 批量插入合并记录
            if (!mergeHoldings.isEmpty()) {
                int insertedCount = mergeHoldingMapper.batchInsert(mergeHoldings);
                logger.info(String.format("✅ 成功插入 %d 条合并持仓记录", insertedCount));
                
                // 5. 验证插入结果
                long totalCount = mergeHoldingMapper.countByFilingId(filingId);
                
                String resultMessage = String.format(
                    "持仓合并任务完成 - FilingId: %d, CIK: %s, 合并后记录数: %d, 原始记录聚合数: %d", 
                    filingId, cik, totalCount, aggregatedData.size()
                );
                
                logger.info("🎉 " + resultMessage);
                return TaskResult.success(resultMessage);
            } else {
                return TaskResult.failure("没有生成合并记录");
            }
            
        } catch (Exception e) {
            logger.error(String.format("💥 持仓合并任务执行失败 - TaskId: %s", task.getTaskId()), e);
            return TaskResult.failure("持仓合并任务执行失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public TaskType getTaskType() {
        return TaskType.HOLDING_MERGE;
    }
    
    /**
     * 安全获取Long值
     */
    private Long getLongValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * 安全获取String值
     */
    private String getStringValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : null;
    }
    
    /**
     * 安全获取BigDecimal值
     */
    private BigDecimal getBigDecimalValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}