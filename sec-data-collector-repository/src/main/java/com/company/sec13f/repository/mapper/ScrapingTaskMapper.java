package com.company.sec13f.repository.mapper;

import com.company.sec13f.repository.entity.ScrapingTask;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * ScrapingTask数据访问接口
 */
public interface ScrapingTaskMapper {
    
    /**
     * 插入新的ScrapingTask记录
     * @param task ScrapingTask实体
     * @return 影响行数
     */
    int insert(ScrapingTask task);
    
    /**
     * 根据ID查询ScrapingTask
     * @param id 主键ID
     * @return ScrapingTask实体
     */
    ScrapingTask selectById(@Param("id") Long id);
    
    /**
     * 根据任务ID查询ScrapingTask
     * @param taskId 任务ID
     * @return ScrapingTask实体
     */
    ScrapingTask selectByTaskId(@Param("taskId") String taskId);
    
    /**
     * 查询所有ScrapingTask（按创建时间倒序）
     * @return ScrapingTask列表
     */
    List<ScrapingTask> selectAll();
    
    /**
     * 根据状态查询ScrapingTask
     * @param status 任务状态
     * @return ScrapingTask列表
     */
    List<ScrapingTask> selectByStatus(@Param("status") ScrapingTask.TaskStatus status);
    
    /**
     * 根据CIK查询ScrapingTask
     * @param cik 公司CIK
     * @return ScrapingTask列表
     */
    List<ScrapingTask> selectByCik(@Param("cik") String cik);
    
    /**
     * 查询失败的任务（用于重试）
     * @return 失败的ScrapingTask列表
     */
    List<ScrapingTask> selectFailedTasks();
    
    /**
     * 查询运行中的任务
     * @return 运行中的ScrapingTask列表
     */
    List<ScrapingTask> selectRunningTasks();
    
    /**
     * 查询最近N个任务
     * @param limit 限制数量
     * @return ScrapingTask列表
     */
    List<ScrapingTask> selectRecentTasks(@Param("limit") int limit);
    
    /**
     * 更新ScrapingTask记录
     * @param task ScrapingTask实体
     * @return 影响行数
     */
    int update(ScrapingTask task);
    
    /**
     * 根据ID删除ScrapingTask
     * @param id 主键ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 根据任务ID删除ScrapingTask
     * @param taskId 任务ID
     * @return 影响行数
     */
    int deleteByTaskId(@Param("taskId") String taskId);
    
    /**
     * 删除已完成的任务
     * @return 影响行数
     */
    int deleteCompletedTasks();
    
    /**
     * 删除指定天数前的旧任务
     * @param days 天数
     * @return 影响行数
     */
    int deleteTasksOlderThan(@Param("days") int days);
    
    /**
     * 统计任务总数
     * @return 总数
     */
    long countAll();
    
    /**
     * 根据状态统计任务数量
     * @param status 任务状态
     * @return 数量
     */
    long countByStatus(@Param("status") ScrapingTask.TaskStatus status);
    
    /**
     * 统计今日任务数量
     * @return 今日任务数量
     */
    long countTodayTasks();
    
    /**
     * 检查任务是否存在
     * @param taskId 任务ID
     * @return 存在返回true，否则返回false
     */
    boolean existsByTaskId(@Param("taskId") String taskId);
}