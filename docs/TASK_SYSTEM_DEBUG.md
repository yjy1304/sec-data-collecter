# 任务系统调试和测试指南

## 问题修复总结

### 已修复的问题

1. **双重任务执行逻辑冲突** ✅
   - 移除了ScrapingController中的直接任务执行逻辑
   - 统一使用TaskService进行任务调度和管理

2. **定时调度未启用** ✅
   - 在主应用类添加了`@EnableScheduling`注解
   - 确保定时任务能够正常运行

3. **日志信息不足** ✅
   - 增强了TaskService中的调试日志
   - 添加了插件注册状态检查
   - 详细记录任务执行的每个步骤

4. **缺少调试工具** ✅
   - 创建了TaskDebugController提供调试接口
   - 可以手动测试任务创建和执行

## 测试步骤

### 1. 启动应用程序

```bash
cd sec-data-collector-web
mvn spring-boot:run
```

### 2. 检查任务系统状态

访问调试接口检查TaskService状态：

```bash
curl -X GET http://localhost:8080/api/tasks/debug/status
```

**期望输出**：
```json
{
  "registeredPlugins": {
    "SCRAP_HOLDING": "ScrapingTaskProcessPlugin"
  },
  "pluginStatusDetails": "TaskService插件状态:\n  - 总数: 1\n  - SCRAP_HOLDING: ScrapingTaskProcessPlugin",
  "scrapingPluginRegistered": true
}
```

### 3. 创建测试任务

```bash
curl -X POST http://localhost:8080/api/tasks/debug/create-test-task
```

**期望输出**：
```json
{
  "success": true,
  "taskId": "uuid-generated-task-id",
  "message": "测试任务已创建",
  "taskParameters": "{\"cik\":\"0001524258\",\"companyName\":\"Alibaba Group (测试)\"}"
}
```

### 4. 手动触发任务调度

```bash
curl -X POST http://localhost:8080/api/tasks/debug/trigger-schedule
```

### 5. 检查任务执行结果

使用上面获得的taskId查询任务状态：

```bash
curl -X GET http://localhost:8080/api/tasks/debug/{taskId}
```

### 6. 查看所有任务

```bash
curl -X GET http://localhost:8080/api/scraping/tasks
```

## 日志监控

启动应用后，观察控制台日志输出，应该能看到以下关键信息：

### 启动时的插件注册日志：
```
🔧 TaskService构造函数调用 - 注入的插件数量: 1
🔍 发现插件: ScrapingTaskProcessPlugin [TaskType: SCRAP_HOLDING]
📌 注册任务处理插件: SCRAP_HOLDING -> ScrapingTaskProcessPlugin
✅ TaskService初始化成功，注册了 1 个插件
```

### 定时调度日志（每分钟）：
```
🕐 开始定时任务调度...
🔍 当前插件注册状态: 1 个插件已注册
🔍 查询到 1 个PENDING任务
📋 发现 1 个待执行任务 (待处理:1, 重试:0)
📝 调度PENDING任务: task-uuid [SCRAP_HOLDING]
```

### 任务执行日志：
```
🎯 开始处理任务: task-uuid [SCRAP_HOLDING] 状态: PENDING
🚀 开始执行任务: task-uuid [SCRAP_HOLDING] 使用插件: ScrapingTaskProcessPlugin
🔧 调用插件处理任务: ScrapingTaskProcessPlugin.handleTask()
📊 插件返回结果: success=true, message=成功爬取并保存了 X 个新的13F文件
✅ 任务完成: task-uuid - 成功爬取并保存了 X 个新的13F文件
💾 更新任务状态到数据库: task-uuid -> COMPLETED
```

## 常见问题排查

### 1. 插件未注册

**症状**：调试状态接口显示`registeredPlugins`为空

**可能原因**：
- ScrapingTaskProcessPlugin没有`@Component`注解
- Spring ComponentScan没有扫描到插件类
- 依赖注入失败

**解决方法**：
```bash
# 检查ScrapingTaskProcessPlugin是否有@Component注解
grep -r "@Component" sec-data-collector-service/src/main/java/com/company/sec13f/service/plugin/
```

### 2. 定时调度不执行

**症状**：没有看到定时调度日志

**可能原因**：
- `@EnableScheduling`注解缺失
- 应用程序配置问题

**解决方法**：
- 确认主应用类有`@EnableScheduling`注解
- 检查application.yml中的调度配置

### 3. 任务一直处于PENDING状态

**症状**：任务创建后状态不改变

**可能原因**：
- 定时调度未运行
- 插件执行异常
- 数据库连接问题

**解决方法**：
- 手动触发调度：`POST /api/tasks/debug/trigger-schedule`
- 查看详细错误日志
- 检查数据库连接状态

### 4. 任务执行失败

**症状**：任务状态变为FAILED

**可能原因**：
- 网络连接问题（抓取SEC数据）
- 数据验证失败
- 数据库写入错误

**解决方法**：
- 查看任务的详细错误信息
- 检查网络连接和SEC API可用性
- 验证数据库表结构

## API接口汇总

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/tasks/debug/status` | GET | 获取任务系统状态 |
| `/api/tasks/debug/create-test-task` | POST | 创建测试任务 |
| `/api/tasks/debug/trigger-schedule` | POST | 手动触发调度 |
| `/api/tasks/debug/{taskId}` | GET | 获取任务详情 |
| `/api/scraping/scrape` | POST | 创建抓取任务 |
| `/api/scraping/tasks` | GET | 查看所有任务 |
| `/api/scraping/status/{taskId}` | GET | 查看任务状态 |
| `/api/scraping/plugin-status` | GET | 获取插件状态 |

## 生产环境注意事项

1. **调试接口安全性**：生产环境应该禁用或保护调试接口
2. **日志级别**：调整日志级别，减少DEBUG日志输出
3. **定时调度频率**：根据实际需求调整Cron表达式
4. **数据库连接池**：确保连接池配置适合生产负载
5. **错误处理**：完善异常处理和告警机制

## 下一步优化建议

1. **监控和指标**：添加Prometheus metrics
2. **任务优先级**：支持任务优先级排序
3. **批量操作**：支持批量任务创建和管理
4. **任务分片**：大型任务自动分片处理
5. **失败重试策略**：更智能的重试机制（指数退避等）