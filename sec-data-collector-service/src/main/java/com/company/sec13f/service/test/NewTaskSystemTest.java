package com.company.sec13f.service.test;

import com.company.sec13f.repository.database.UniversalTaskDAO;
import com.company.sec13f.repository.entity.Task;
import com.company.sec13f.repository.enums.TaskStatus;
import com.company.sec13f.repository.enums.TaskType;
import com.company.sec13f.service.TaskService;
import com.company.sec13f.service.plugin.TaskParameters;

import java.util.List;

/**
 * 测试新的任务管理系统
 */
public class NewTaskSystemTest {
    
    public static void main(String[] args) {
        try {
            System.out.println("🧪 开始测试新的任务管理系统...");
            
            // 1. 测试UniversalTaskDAO
            UniversalTaskDAO taskDAO = new UniversalTaskDAO();
            taskDAO.initializeTasksTable();
            System.out.println("✅ 数据库表初始化成功");
            
            // 2. 创建测试任务
            Task testTask = new Task("test_task_001", TaskType.SCRAP_HOLDING);
            TaskParameters params = TaskParameters.forScraping("0001524258", "Alibaba Group");
            testTask.setTaskParameters(params.toJson());
            
            // 3. 保存任务
            taskDAO.saveTask(testTask);
            System.out.println("✅ 测试任务保存成功: " + testTask.getTaskId());
            
            // 4. 查询任务
            Task retrievedTask = taskDAO.findByTaskId(testTask.getTaskId());
            if (retrievedTask != null) {
                System.out.println("✅ 任务查询成功: " + retrievedTask);
            } else {
                System.out.println("❌ 任务查询失败");
            }
            
            // 5. 查询所有任务
            List<Task> allTasks = taskDAO.findAllTasks();
            System.out.println("✅ 查询到所有任务数量: " + allTasks.size());
            
            // 6. 测试任务状态统计
            long totalCount = taskDAO.countTasks();
            long pendingCount = taskDAO.countTasksByStatus(TaskStatus.PENDING);
            System.out.println("✅ 任务统计 - 总数: " + totalCount + ", 待处理: " + pendingCount);
            
            // 7. 测试TaskService
            TaskService taskService = new TaskService(taskDAO);
            taskService.init();
            
            String newTaskId = taskService.createTask(TaskType.SCRAP_HOLDING, params.toJson());
            System.out.println("✅ 通过TaskService创建任务: " + newTaskId);
            
            // 8. 查询TaskService创建的任务
            Task newTask = taskService.getTaskStatus(newTaskId);
            if (newTask != null) {
                System.out.println("✅ TaskService任务查询成功: " + newTask.getTaskId());
            }
            
            System.out.println("🎉 新任务管理系统测试完成！");
            
        } catch (Exception e) {
            System.err.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}