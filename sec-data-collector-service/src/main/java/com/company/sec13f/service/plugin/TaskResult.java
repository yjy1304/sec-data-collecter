package com.company.sec13f.service.plugin;

/**
 * 任务执行结果
 */
public class TaskResult {
    
    private boolean success;
    private String message;
    private Throwable error;
    
    private TaskResult(boolean success, String message, Throwable error) {
        this.success = success;
        this.message = message;
        this.error = error;
    }
    
    public static TaskResult success(String message) {
        return new TaskResult(true, message, null);
    }
    
    public static TaskResult failure(String message) {
        return new TaskResult(false, message, null);
    }
    
    public static TaskResult failure(String message, Throwable error) {
        return new TaskResult(false, message, error);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public Throwable getError() {
        return error;
    }
    
    @Override
    public String toString() {
        return "TaskResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", error=" + error +
                '}';
    }
}