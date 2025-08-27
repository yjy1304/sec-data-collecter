package com.company.sec13f.repository.entity;

import java.time.LocalDateTime;

/**
 * 数据抓取任务实体类
 * 对应数据库表：scraping_tasks
 */
public class ScrapingTask {
    
    private Long id;
    private String taskId;
    private String cik;
    private String companyName;
    private TaskStatus status;
    private String message;
    private String errorMessage;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer savedFilings;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * 任务状态枚举
     */
    public enum TaskStatus {
        PENDING("PENDING"),
        RUNNING("RUNNING"),
        COMPLETED("COMPLETED"),
        FAILED("FAILED");
        
        private final String value;
        
        TaskStatus(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        public static TaskStatus fromValue(String value) {
            for (TaskStatus status : TaskStatus.values()) {
                if (status.value.equals(value)) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Unknown TaskStatus value: " + value);
        }
    }
    
    // 构造方法
    public ScrapingTask() {
    }
    
    public ScrapingTask(String taskId, String cik, String companyName) {
        this.taskId = taskId;
        this.cik = cik;
        this.companyName = companyName;
        this.status = TaskStatus.PENDING;
        this.message = "任务已创建";
        this.savedFilings = 0;
    }
    
    // 计算任务执行时长（秒）
    public long getDurationSeconds() {
        if (startTime == null) return 0;
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
        return java.time.Duration.between(startTime, end).getSeconds();
    }
    
    // Getter和Setter方法
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    public String getCik() {
        return cik;
    }
    
    public void setCik(String cik) {
        this.cik = cik;
    }
    
    public String getCompanyName() {
        return companyName;
    }
    
    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }
    
    public TaskStatus getStatus() {
        return status;
    }
    
    public void setStatus(TaskStatus status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    
    public Integer getSavedFilings() {
        return savedFilings;
    }
    
    public void setSavedFilings(Integer savedFilings) {
        this.savedFilings = savedFilings;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    @Override
    public String toString() {
        return "ScrapingTask{" +
                "id=" + id +
                ", taskId='" + taskId + '\'' +
                ", cik='" + cik + '\'' +
                ", companyName='" + companyName + '\'' +
                ", status=" + status +
                ", message='" + message + '\'' +
                ", savedFilings=" + savedFilings +
                ", durationSeconds=" + getDurationSeconds() +
                '}';
    }
}