package com.company.sec13f.repository.mapper;

import com.company.sec13f.repository.entity.Task;
import com.company.sec13f.repository.enums.TaskStatus;
import com.company.sec13f.repository.enums.TaskType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通用任务表数据访问接口
 */
@Mapper
public interface TaskMapper {
    
    /**
     * 插入新任务
     */
    int insert(Task task);
    
    /**
     * 根据ID查询任务
     */
    Task selectById(Long id);
    
    /**
     * 根据任务ID查询任务
     */
    Task selectByTaskId(String taskId);
    
    /**
     * 查询所有任务
     */
    List<Task> selectAll();
    
    /**
     * 根据状态查询任务
     */
    List<Task> selectByStatus(@Param("status") String status);
    
    /**
     * 根据任务类型查询任务
     */
    List<Task> selectByTaskType(@Param("taskType") String taskType);
    
    /**
     * 查询待处理的任务
     */
    List<Task> selectPendingTasks();
    
    /**
     * 查询可以重试的任务
     */
    List<Task> selectRetryTasksReadyForExecution(@Param("currentTime") String currentTime);
    
    /**
     * 更新任务
     */
    int update(Task task);
    
    /**
     * 根据ID删除任务
     */
    int deleteById(Long id);
    
    /**
     * 根据任务ID删除任务
     */
    int deleteByTaskId(String taskId);
    
    /**
     * 删除所有已完成的任务
     */
    int deleteCompletedTasks();
    
    /**
     * 删除指定天数之前的任务
     */
    int deleteTasksOlderThan(@Param("days") int days);
    
    /**
     * 统计任务总数
     */
    long countAll();
    
    /**
     * 根据状态统计任务数
     */
    long countByStatus(@Param("status") String status);
    
    /**
     * 检查任务ID是否存在
     */
    boolean existsByTaskId(String taskId);
}