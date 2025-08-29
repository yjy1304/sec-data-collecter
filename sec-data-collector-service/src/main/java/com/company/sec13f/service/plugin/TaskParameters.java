package com.company.sec13f.service.plugin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * 任务参数工具类
 * 用于处理任务参数的序列化和反序列化
 */
public class TaskParameters {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Object> parameters;
    
    public TaskParameters() {
        this.parameters = new HashMap<>();
    }
    
    public TaskParameters(String json) {
        try {
            if (json != null && !json.trim().isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> parsed = objectMapper.readValue(json, Map.class);
                this.parameters = parsed;
            } else {
                this.parameters = new HashMap<>();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse task parameters JSON: " + json, e);
        }
    }
    
    public TaskParameters put(String key, Object value) {
        parameters.put(key, value);
        return this;
    }
    
    public String getString(String key) {
        Object value = parameters.get(key);
        return value != null ? value.toString() : null;
    }
    
    public Integer getInteger(String key) {
        Object value = parameters.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    public Boolean getBoolean(String key) {
        Object value = parameters.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return null;
    }
    
    public String toJson() {
        try {
            return objectMapper.writeValueAsString(parameters);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize task parameters to JSON", e);
        }
    }
    
    public boolean isEmpty() {
        return parameters.isEmpty();
    }
    
    public Map<String, Object> getAll() {
        return new HashMap<>(parameters);
    }
    
    @Override
    public String toString() {
        return toJson();
    }
    
    // 静态工厂方法用于创建数据抓取任务参数
    public static TaskParameters forScraping(String cik, String companyName) {
        return new TaskParameters()
            .put("cik", cik)
            .put("companyName", companyName);
    }
}