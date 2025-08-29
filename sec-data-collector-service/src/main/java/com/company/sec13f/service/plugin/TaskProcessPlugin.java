package com.company.sec13f.service.plugin;

import com.company.sec13f.repository.entity.Task;
import com.company.sec13f.repository.enums.TaskType;

/**
 * 任务处理插件接口
 * 不同类型的任务通过实现此接口来提供具体的处理逻辑
 */
public interface TaskProcessPlugin {
    
    /**
     * 执行任务
     * @param task 任务实体
     * @return 任务执行结果
     */
    TaskResult handleTask(Task task);
    
    /**
     * 返回该插件支持的任务类型
     * @return 任务类型
     */
    TaskType getTaskType();
}