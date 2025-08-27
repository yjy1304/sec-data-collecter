package com.company.sec13f.service.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    private static final String LOG_FILE = "sec13f-parser.log";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static Logger instance;
    private PrintWriter logWriter;
    
    private Logger() {
        try {
            logWriter = new PrintWriter(new FileWriter(LOG_FILE, true));
        } catch (IOException e) {
            System.err.println("Failed to initialize log file: " + e.getMessage());
        }
    }
    
    public static synchronized Logger getInstance() {
        if (instance == null) {
            instance = new Logger();
        }
        return instance;
    }
    
    public void info(String message) {
        log("INFO", message);
    }
    
    public void warn(String message) {
        log("WARN", message);
    }
    
    public void error(String message) {
        log("ERROR", message);
    }
    
    public void error(String message, Throwable throwable) {
        log("ERROR", message + " - " + throwable.getMessage());
        if (logWriter != null) {
            throwable.printStackTrace(logWriter);
            logWriter.flush();
        }
    }
    
    public void debug(String message) {
        log("DEBUG", message);
    }
    
    private void log(String level, String message) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String logEntry = String.format("[%s] %s - %s", timestamp, level, message);
        
        // 输出到控制台
        System.out.println(logEntry);
        
        // 输出到文件
        if (logWriter != null) {
            logWriter.println(logEntry);
            logWriter.flush();
        }
    }
    
    public void close() {
        if (logWriter != null) {
            logWriter.close();
        }
    }
    
    // 爬取相关的专用日志方法
    public void scrapingStarted(String cik, String companyName) {
        info(String.format("SCRAPING_STARTED - CIK: %s, Company: %s", cik, companyName));
    }
    
    public void scrapingCompleted(String cik, int savedFilings) {
        info(String.format("SCRAPING_COMPLETED - CIK: %s, Saved: %d filings", cik, savedFilings));
    }
    
    public void scrapingFailed(String cik, String error) {
        error(String.format("SCRAPING_FAILED - CIK: %s, Error: %s", cik, error));
    }
    
    public void apiRequest(String endpoint, String parameters) {
        debug(String.format("API_REQUEST - Endpoint: %s, Params: %s", endpoint, parameters));
    }
    
    public void dataValidation(String type, String message) {
        info(String.format("DATA_VALIDATION - Type: %s, Message: %s", type, message));
    }
    
    public void secRequest(String url, int statusCode) {
        debug(String.format("SEC_REQUEST - URL: %s, Status: %d", url, statusCode));
    }
}