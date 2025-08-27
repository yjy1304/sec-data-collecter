package com.company.sec13f.repository.database;

import com.company.sec13f.repository.enums.TaskStatus;
import com.company.sec13f.repository.model.ScrapingTask;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for managing scraping tasks in the database
 */
public class TaskDAO {
    private static final String DB_URL = "jdbc:sqlite:sec13f.db";
    
    // SQL statements for tasks table
    private static final String CREATE_TASKS_TABLE = 
        "CREATE TABLE IF NOT EXISTS scraping_tasks (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "task_id TEXT NOT NULL UNIQUE, " +
        "cik TEXT NOT NULL, " +
        "company_name TEXT NOT NULL, " +
        "status TEXT NOT NULL, " +
        "message TEXT, " +
        "error_message TEXT, " +
        "start_time TIMESTAMP, " +
        "end_time TIMESTAMP, " +
        "saved_filings INTEGER DEFAULT 0, " +
        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
        
    private static final String INSERT_TASK = 
        "INSERT INTO scraping_tasks (task_id, cik, company_name, status, message, start_time) " +
        "VALUES (?, ?, ?, ?, ?, ?)";
        
    private static final String UPDATE_TASK = 
        "UPDATE scraping_tasks SET status = ?, message = ?, error_message = ?, " +
        "end_time = ?, saved_filings = ?, updated_at = CURRENT_TIMESTAMP " +
        "WHERE task_id = ?";
        
    private static final String SELECT_ALL_TASKS = 
        "SELECT * FROM scraping_tasks ORDER BY created_at DESC";
        
    private static final String SELECT_TASK_BY_ID = 
        "SELECT * FROM scraping_tasks WHERE task_id = ?";
        
    private static final String DELETE_COMPLETED_TASKS = 
        "DELETE FROM scraping_tasks WHERE status = 'COMPLETED'";
        
    private static final String DELETE_TASK_BY_ID = 
        "DELETE FROM scraping_tasks WHERE task_id = ?";
    
    /**
     * Initialize the tasks table
     */
    public void initializeTasksTable() throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(CREATE_TASKS_TABLE);
        }
    }
    
    /**
     * Save a new task to the database
     */
    public void saveTask(ScrapingTask task) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(INSERT_TASK)) {
            
            pstmt.setString(1, task.getTaskId());
            pstmt.setString(2, task.getCik());
            pstmt.setString(3, task.getCompanyName());
            pstmt.setString(4, task.getStatus().toString());
            pstmt.setString(5, task.getMessage());
            pstmt.setTimestamp(6, task.getStartTime() != null ? 
                Timestamp.valueOf(task.getStartTime()) : null);
            
            pstmt.executeUpdate();
        }
    }
    
    /**
     * Update an existing task in the database
     */
    public void updateTask(ScrapingTask task) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(UPDATE_TASK)) {
            
            pstmt.setString(1, task.getStatus().toString());
            pstmt.setString(2, task.getMessage());
            pstmt.setString(3, task.getError());
            pstmt.setTimestamp(4, task.getEndTime() != null ? 
                Timestamp.valueOf(task.getEndTime()) : null);
            pstmt.setInt(5, task.getSavedFilings());
            pstmt.setString(6, task.getTaskId());
            
            pstmt.executeUpdate();
        }
    }
    
    /**
     * Get all tasks from the database
     */
    public List<ScrapingTask> getAllTasks() throws SQLException {
        List<ScrapingTask> tasks = new ArrayList<>();
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SELECT_ALL_TASKS)) {
            
            while (rs.next()) {
                ScrapingTask task = createTaskFromResultSet(rs);
                tasks.add(task);
            }
        }
        
        return tasks;
    }
    
    /**
     * Get a specific task by task ID
     */
    public ScrapingTask getTaskById(String taskId) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(SELECT_TASK_BY_ID)) {
            
            pstmt.setString(1, taskId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return createTaskFromResultSet(rs);
                }
            }
        }
        
        return null;
    }
    
    /**
     * Delete only completed tasks (preserve failed tasks for history)
     */
    public int deleteCompletedTasks() throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            
            return stmt.executeUpdate(DELETE_COMPLETED_TASKS);
        }
    }
    
    /**
     * Delete a specific task by ID
     */
    public boolean deleteTask(String taskId) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(DELETE_TASK_BY_ID)) {
            
            pstmt.setString(1, taskId);
            return pstmt.executeUpdate() > 0;
        }
    }
    
    /**
     * Create a ScrapingStatus object from a ResultSet
     */
    private ScrapingTask createTaskFromResultSet(ResultSet rs) throws SQLException {
        String taskId = rs.getString("task_id");
        String cik = rs.getString("cik");
        String companyName = rs.getString("company_name");
        
        ScrapingTask task = new ScrapingTask(taskId, cik, companyName);
        
        // Set status
        String statusStr = rs.getString("status");
        task.setStatus(TaskStatus.valueOf(statusStr));
        
        // Set other fields
        task.setMessage(rs.getString("message"));
        task.setError(rs.getString("error_message"));
        task.setSavedFilings(rs.getInt("saved_filings"));
        
        // Set timestamps
        Timestamp startTime = rs.getTimestamp("start_time");
        if (startTime != null) {
            task.setStartTime(startTime.toLocalDateTime());
        }
        
        Timestamp endTime = rs.getTimestamp("end_time");
        if (endTime != null) {
            task.setEndTime(endTime.toLocalDateTime());
        }
        
        return task;
    }
}