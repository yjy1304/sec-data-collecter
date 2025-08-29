package com.company.sec13f.service.config;

import com.company.sec13f.service.TaskService;
import com.company.sec13f.service.plugin.ScrapingTaskProcessPlugin;
import com.company.sec13f.service.util.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

// import javax.annotation.PostConstruct; // Not available in Java 8

/**
 * TaskService配置类
 * 负责自动注册任务处理插件
 */
@Configuration
public class TaskServiceConfig {
    
    private final TaskService taskService;
    private final ScrapingTaskProcessPlugin scrapingPlugin;
    private final Logger logger = Logger.getInstance();
    
    @Autowired
    public TaskServiceConfig(TaskService taskService, ScrapingTaskProcessPlugin scrapingPlugin) {
        this.taskService = taskService;
        this.scrapingPlugin = scrapingPlugin;
    }
    
    // @PostConstruct // Not available in Java 8
    public void registerPlugins() {
        try {
            // 注册数据抓取插件
            taskService.registerPlugin(scrapingPlugin);
            logger.info("✅ 注册任务处理插件: " + scrapingPlugin.getTaskType());
            
        } catch (Exception e) {
            logger.error("❌ 注册任务处理插件失败", e);
        }
    }
}