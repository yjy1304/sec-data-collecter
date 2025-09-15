package com.company.sec13f.repository.enums;

/**
 * 任务类型枚举
 */
public enum TaskType {
    SEC_SCRAPING,           // SEC数据抓取
    DATA_ANALYSIS,          // 数据分析
    DATA_EXPORT,            // 数据导出
    SYSTEM_MAINTENANCE,     // 系统维护
    SCRAP_HOLDING,          // 抓取持仓（兼容性）
    SCRAP_FINANCIAL_REPORT, // 抓取财报（兼容性）
    HOLDING_MERGE           // 持仓数据合并
}