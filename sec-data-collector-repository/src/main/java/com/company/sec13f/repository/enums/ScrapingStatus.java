package com.company.sec13f.repository.enums;

/**
 * 数据抓取状态枚举
 */
public enum ScrapingStatus {
    PENDING("待处理"),
    RUNNING("执行中"),
    COMPLETED("已完成"),
    FAILED("失败");
    
    private final String description;
    
    ScrapingStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return description;
    }
}