package com.company.sec13f.repository.database;

import com.company.sec13f.repository.MyBatisSessionFactory;
import com.company.sec13f.repository.mapper.ScrapingTaskMapper;
import com.company.sec13f.repository.model.ScrapingTask;
import org.apache.ibatis.session.SqlSession;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for managing scraping tasks in the database
 * Uses MyBatis for data access
 */
public class TaskDAO {
    
    /**
     * Initialize the tasks table using MyBatis
     */
    public void initializeTasksTable() throws SQLException {
        // Table creation is handled by DatabaseInitMapper or schema scripts
        try (SqlSession session = MyBatisSessionFactory.getSqlSessionFactory().openSession()) {
            // Ensure table exists
            session.commit();
        }
    }
    
    /**
     * Save a new task to the database using MyBatis
     */
    public void saveTask(ScrapingTask task) throws SQLException {
        try (SqlSession session = MyBatisSessionFactory.getSqlSessionFactory().openSession()) {
            ScrapingTaskMapper mapper = session.getMapper(ScrapingTaskMapper.class);
            
            // Convert model.ScrapingTask to entity.ScrapingTask
            com.company.sec13f.repository.entity.ScrapingTask entityTask = convertToEntity(task);
            mapper.insert(entityTask);
            session.commit();
        }
    }
    
    /**
     * Update an existing task in the database using MyBatis
     */
    public void updateTask(ScrapingTask task) throws SQLException {
        try (SqlSession session = MyBatisSessionFactory.getSqlSessionFactory().openSession()) {
            ScrapingTaskMapper mapper = session.getMapper(ScrapingTaskMapper.class);
            
            // Convert model.ScrapingTask to entity.ScrapingTask
            com.company.sec13f.repository.entity.ScrapingTask entityTask = convertToEntity(task);
            mapper.update(entityTask);
            session.commit();
        }
    }
    
    /**
     * Get all tasks from the database using MyBatis
     */
    public List<ScrapingTask> getAllTasks() throws SQLException {
        try (SqlSession session = MyBatisSessionFactory.getSqlSessionFactory().openSession()) {
            ScrapingTaskMapper mapper = session.getMapper(ScrapingTaskMapper.class);
            
            List<com.company.sec13f.repository.entity.ScrapingTask> entityTasks = mapper.selectAll();
            List<ScrapingTask> modelTasks = new ArrayList<>();
            
            for (com.company.sec13f.repository.entity.ScrapingTask entityTask : entityTasks) {
                modelTasks.add(convertToModel(entityTask));
            }
            
            return modelTasks;
        }
    }
    
    /**
     * Get a task by its task ID using MyBatis
     */
    public ScrapingTask getTaskById(String taskId) throws SQLException {
        try (SqlSession session = MyBatisSessionFactory.getSqlSessionFactory().openSession()) {
            ScrapingTaskMapper mapper = session.getMapper(ScrapingTaskMapper.class);
            
            com.company.sec13f.repository.entity.ScrapingTask entityTask = mapper.selectByTaskId(taskId);
            return entityTask != null ? convertToModel(entityTask) : null;
        }
    }
    
    /**
     * Delete completed tasks using MyBatis
     */
    public int deleteCompletedTasks() throws SQLException {
        try (SqlSession session = MyBatisSessionFactory.getSqlSessionFactory().openSession()) {
            ScrapingTaskMapper mapper = session.getMapper(ScrapingTaskMapper.class);
            
            int result = mapper.deleteCompletedTasks();
            session.commit();
            return result;
        }
    }
    
    /**
     * Convert model ScrapingTask to entity ScrapingTask
     */
    private com.company.sec13f.repository.entity.ScrapingTask convertToEntity(
            ScrapingTask modelTask) {
        com.company.sec13f.repository.entity.ScrapingTask entityTask = 
            new com.company.sec13f.repository.entity.ScrapingTask();
        
        entityTask.setTaskId(modelTask.getTaskId());
        entityTask.setCik(modelTask.getCik());
        entityTask.setCompanyName(modelTask.getCompanyName());
        entityTask.setStatus(convertModelStatusToEntityStatus(modelTask.getStatus()));
        entityTask.setMessage(modelTask.getMessage());
        entityTask.setErrorMessage(modelTask.getError());
        entityTask.setStartTime(modelTask.getStartTime());
        entityTask.setEndTime(modelTask.getEndTime());
        entityTask.setSavedFilings(modelTask.getSavedFilings());
        
        return entityTask;
    }
    
    /**
     * Convert entity ScrapingTask to model ScrapingTask
     */
    private ScrapingTask convertToModel(
            com.company.sec13f.repository.entity.ScrapingTask entityTask) {
        ScrapingTask modelTask = new ScrapingTask(
            entityTask.getTaskId(),
            entityTask.getCik(), 
            entityTask.getCompanyName()
        );
        
        modelTask.setStatus(convertEntityStatusToModelStatus(entityTask.getStatus()));
        modelTask.setMessage(entityTask.getMessage());
        modelTask.setError(entityTask.getErrorMessage());
        modelTask.setStartTime(entityTask.getStartTime());
        modelTask.setEndTime(entityTask.getEndTime());
        modelTask.setSavedFilings(entityTask.getSavedFilings());
        
        return modelTask;
    }
    
    /**
     * Convert model TaskStatus to entity TaskStatus
     */
    private com.company.sec13f.repository.entity.ScrapingTask.TaskStatus convertModelStatusToEntityStatus(
            com.company.sec13f.repository.enums.TaskStatus modelStatus) {
        if (modelStatus == null) return null;
        
        switch (modelStatus) {
            case PENDING:
                return com.company.sec13f.repository.entity.ScrapingTask.TaskStatus.PENDING;
            case RUNNING:
                return com.company.sec13f.repository.entity.ScrapingTask.TaskStatus.RUNNING;
            case COMPLETED:
                return com.company.sec13f.repository.entity.ScrapingTask.TaskStatus.COMPLETED;
            case FAILED:
                return com.company.sec13f.repository.entity.ScrapingTask.TaskStatus.FAILED;
            default:
                throw new IllegalArgumentException("Unknown model TaskStatus: " + modelStatus);
        }
    }
    
    /**
     * Convert entity TaskStatus to model TaskStatus
     */
    private com.company.sec13f.repository.enums.TaskStatus convertEntityStatusToModelStatus(
            com.company.sec13f.repository.entity.ScrapingTask.TaskStatus entityStatus) {
        if (entityStatus == null) return null;
        
        switch (entityStatus) {
            case PENDING:
                return com.company.sec13f.repository.enums.TaskStatus.PENDING;
            case RUNNING:
                return com.company.sec13f.repository.enums.TaskStatus.RUNNING;
            case COMPLETED:
                return com.company.sec13f.repository.enums.TaskStatus.COMPLETED;
            case FAILED:
                return com.company.sec13f.repository.enums.TaskStatus.FAILED;
            default:
                throw new IllegalArgumentException("Unknown entity TaskStatus: " + entityStatus);
        }
    }
}