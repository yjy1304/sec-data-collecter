package com.company.sec13f.repository.enums;

/**
 * 任务状态枚举
 */
public enum TaskStatus {
    PENDING("待处理"),
    RETRY("等待重试"),
    COMPLETED("已完成"),
    FAILED("失败");
    
    private final String description;
    
    TaskStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return name(); // 返回枚举名称而不是中文描述，确保数据库序列化/反序列化正常
    }
}