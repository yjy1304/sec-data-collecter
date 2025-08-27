package com.company.sec13f.repository.model;

import com.company.sec13f.repository.enums.TaskStatus;
import java.time.LocalDateTime;

/**
 * 数据抓取任务实体类
 */
public class ScrapingTask {
    private String taskId;
    private String cik;
    private String companyName;
    private TaskStatus status;
    private String message;
    private String error;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int savedFilings;
    
    // 默认构造函数
    public ScrapingTask() {}
    
    // 构造函数
    public ScrapingTask(String taskId, String cik, String companyName) {
        this.taskId = taskId;
        this.cik = cik;
        this.companyName = companyName;
        this.status = TaskStatus.PENDING;
        this.savedFilings = 0;
    }
    
    // Getters and Setters
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
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
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
    
    public int getSavedFilings() {
        return savedFilings;
    }
    
    public void setSavedFilings(int savedFilings) {
        this.savedFilings = savedFilings;
    }
    
    @Override
    public String toString() {
        return "ScrapingTask{" +
                "taskId='" + taskId + '\'' +
                ", cik='" + cik + '\'' +
                ", companyName='" + companyName + '\'' +
                ", status=" + status +
                ", message='" + message + '\'' +
                ", error='" + error + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", savedFilings=" + savedFilings +
                '}';
    }
}