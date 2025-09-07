package com.company.sec13f.repository.entity;

import com.company.sec13f.repository.enums.TaskStatus;
import com.company.sec13f.repository.enums.TaskType;
import java.time.LocalDateTime;

/**
 * 通用任务实体类
 * 对应数据库表：tasks
 */
public class Task {
    
    private Long id;                      // 数据库自增主键
    private String taskId;                // 当前任务ID，一般使用UUID
    private TaskType taskType;            // 任务类型枚举
    private TaskStatus status;            // 任务状态枚举
    private String message;               // 当前任务执行结果信息
    private String taskParameters;        // 任务参数，JSON格式存储
    private Integer retryTimes;           // 重试次数
    private LocalDateTime startTime;      // 任务开始时间
    private LocalDateTime nextExecuteTime; // 下次执行时间
    private LocalDateTime endTime;        // 任务结束时间
    private LocalDateTime createdAt;      // 创建时间
    private LocalDateTime updatedAt;      // 更新时间
    
    // 构造方法
    public Task() {
    }
    
    public Task(String taskId, TaskType taskType) {
        this.taskId = taskId;
        this.taskType = taskType;
        this.status = TaskStatus.PENDING;
        this.message = "任务已创建";
        this.retryTimes = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // 计算任务执行时长（秒）
    public long getDurationSeconds() {
        if (startTime == null) return 0;
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
        return java.time.Duration.between(startTime, end).getSeconds();
    }
    
    // 判断任务是否需要重试
    public boolean needsRetry(int maxRetries) {
        return status == TaskStatus.FAILED && retryTimes < maxRetries;
    }
    
    // 设置重试状态
    public void setForRetry(String errorMessage) {
        this.status = TaskStatus.RETRY;
        this.message = errorMessage;
        this.retryTimes = (retryTimes == null ? 0 : retryTimes) + 1;
        this.nextExecuteTime = LocalDateTime.now().plusHours(1); // 1小时后重试
        this.updatedAt = LocalDateTime.now();
    }

    
    // 设置任务完成
    public void setCompleted(String message) {
        this.status = TaskStatus.COMPLETED;
        this.message = message;
        this.endTime = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // 设置任务失败
    public void setFailed(String errorMessage) {
        this.status = TaskStatus.FAILED;
        this.message = errorMessage;
        this.endTime = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
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
    
    public TaskType getTaskType() {
        return taskType;
    }
    
    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
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
    
    public String getTaskParameters() {
        return taskParameters;
    }
    
    public void setTaskParameters(String taskParameters) {
        this.taskParameters = taskParameters;
    }
    
    public Integer getRetryTimes() {
        return retryTimes;
    }
    
    public void setRetryTimes(Integer retryTimes) {
        this.retryTimes = retryTimes;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalDateTime getNextExecuteTime() {
        return nextExecuteTime;
    }
    
    public void setNextExecuteTime(LocalDateTime nextExecuteTime) {
        this.nextExecuteTime = nextExecuteTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
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
        return "Task{" +
                "id=" + id +
                ", taskId='" + taskId + '\'' +
                ", taskType=" + taskType +
                ", status=" + status +
                ", message='" + message + '\'' +
                ", retryTimes=" + retryTimes +
                ", durationSeconds=" + getDurationSeconds() +
                '}';
    }
}