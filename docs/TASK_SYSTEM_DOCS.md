# SEC Data Collector 任务系统文档

## 1. 概述

### 1.1 系统目标

SEC Data Collector 任务系统是一个基于Spring Boot的异步任务处理框架，专门设计用于处理SEC数据抓取、分析和导出等长时间运行的任务。该系统具备以下核心特性：

- **异步执行**：所有任务均异步执行，避免阻塞主线程
- **自动调度**：基于Cron表达式的定时任务调度
- **智能重试**：失败任务自动重试机制，支持指数退避策略
- **插件化架构**：可扩展的任务处理插件系统
- **状态管理**：完整的任务生命周期状态跟踪
- **并发控制**：线程池管理，支持并发任务执行

### 1.2 架构概述

```
┌─────────────────────────────────────────────────────────────┐
│                     Task System Architecture                │
├─────────────────────────────────────────────────────────────┤
│  Web Layer          │  TaskController (REST API)           │
├─────────────────────────────────────────────────────────────┤
│  Service Layer       │  TaskService (Core Logic)            │
│                      │  - Plugin Management                 │
│                      │  - Scheduling                        │
│                      │  - Execution Control                 │
├─────────────────────────────────────────────────────────────┤
│  Plugin Layer        │  TaskProcessPlugin Interface         │
│                      │  - ScrapingTaskProcessPlugin         │
│                      │  - [Future Plugins...]               │
├─────────────────────────────────────────────────────────────┤
│  Repository Layer    │  TaskMapper (MyBatis)               │
│                      │  Task Entity & Enums                │
├─────────────────────────────────────────────────────────────┤
│  Database Layer      │  MySQL (tasks table)                │
└─────────────────────────────────────────────────────────────┘
```

## 2. 数据库模型结构

### 2.1 Tasks 表结构

```sql
CREATE TABLE tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id VARCHAR(255) NOT NULL UNIQUE,
    task_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    message TEXT,
    task_parameters TEXT,
    retry_times INT DEFAULT 0,
    start_time TIMESTAMP NULL,
    next_execute_time TIMESTAMP NULL,
    end_time TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_task_id (task_id),
    INDEX idx_status (status),
    INDEX idx_task_type (task_type),
    INDEX idx_next_execute_time (next_execute_time),
    INDEX idx_created_at (created_at)
);
```

### 2.2 字段说明

| 字段名 | 类型 | 说明 | 示例 |
|--------|------|------|------|
| `id` | BIGINT | 数据库自增主键 | 1, 2, 3... |
| `task_id` | VARCHAR(255) | 任务唯一标识符(UUID) | `scrape_0001524258_1692308734567` |
| `task_type` | VARCHAR(50) | 任务类型(枚举值) | `SEC_SCRAPING`, `DATA_ANALYSIS` |
| `status` | VARCHAR(20) | 任务状态(枚举值) | `PENDING`, `COMPLETED`, `FAILED`, `RETRY` |
| `message` | TEXT | 任务执行结果信息 | "成功爬取并保存了 15 个新的13F文件" |
| `task_parameters` | TEXT | 任务参数(JSON格式) | `{"cik":"0001524258","companyName":"Alibaba"}` |
| `retry_times` | INT | 重试次数计数器 | 0, 1, 2, 3... |
| `start_time` | TIMESTAMP | 任务开始执行时间 | `2023-08-18 14:30:00` |
| `next_execute_time` | TIMESTAMP | 下次执行时间(重试用) | `2023-08-18 15:30:00` |
| `end_time` | TIMESTAMP | 任务结束时间 | `2023-08-18 14:45:00` |
| `created_at` | TIMESTAMP | 记录创建时间 | `2023-08-18 14:25:00` |
| `updated_at` | TIMESTAMP | 记录更新时间 | `2023-08-18 14:45:00` |

### 2.3 任务状态枚举 (TaskStatus)

```java
public enum TaskStatus {
    PENDING("待处理"),    // 任务已创建，等待执行
    RETRY("等待重试"),    // 任务失败，等待重试
    COMPLETED("已完成"),  // 任务执行成功
    FAILED("失败");       // 任务执行失败，不再重试
}
```

### 2.4 任务类型枚举 (TaskType)

```java
public enum TaskType {
    SEC_SCRAPING,           // SEC数据抓取
    DATA_ANALYSIS,          // 数据分析
    DATA_EXPORT,            // 数据导出
    SYSTEM_MAINTENANCE,     // 系统维护
    SCRAP_HOLDING,          // 抓取持仓（兼容性）
    SCRAP_FINANCIAL_REPORT  // 抓取财报（兼容性）
}
```

## 3. 任务状态管理

### 3.1 任务生命周期

```
    ┌─────────┐
    │ PENDING │ ──────┐
    └─────────┘       │
         │            │
         ▼            │
    ┌──────────┐      │
    │ 执行任务  │      │
    └──────────┘      │
         │            │
         ▼            │
    ┌─────────────┐   │
    │ 成功/失败？  │   │
    └─────────────┘   │
         │            │
      成功 │ 失败       │
         ▼            ▼
    ┌───────────┐ ┌─────────┐
    │ COMPLETED │ │  需重试？ │
    └───────────┘ └─────────┘
                       │
                    是 │ 否
                       ▼
                  ┌────────┐    ┌────────┐
                  │ RETRY  │    │ FAILED │
                  └────────┘    └────────┘
                       │
                       │ (时间到期)
                       └──────────┘
```

### 3.2 状态转换逻辑

#### 3.2.1 任务创建
```java
Task task = new Task(taskId, taskType);
// 默认状态：PENDING
// 默认消息："任务已创建"
// 重试次数：0
```

#### 3.2.2 任务完成
```java
public void setCompleted(String message) {
    this.status = TaskStatus.COMPLETED;
    this.message = message;
    this.endTime = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
}
```

#### 3.2.3 任务失败
```java
public void setFailed(String errorMessage) {
    this.status = TaskStatus.FAILED;
    this.message = errorMessage;
    this.endTime = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
}
```

#### 3.2.4 任务重试
```java
public void setForRetry(String errorMessage) {
    this.status = TaskStatus.RETRY;
    this.message = errorMessage;
    this.retryTimes = (retryTimes == null ? 0 : retryTimes) + 1;
    this.nextExecuteTime = LocalDateTime.now().plusHours(1); // 1小时后重试
    this.updatedAt = LocalDateTime.now();
}
```

### 3.3 重试机制

- **最大重试次数**：3次
- **重试间隔**：1小时
- **重试条件**：`status == TaskStatus.FAILED && retryTimes < maxRetries`
- **重试策略**：固定间隔（可扩展为指数退避）

```java
// 检查任务是否需要重试
public boolean needsRetry(int maxRetries) {
    return status == TaskStatus.FAILED && retryTimes < maxRetries;
}
```

## 4. 任务框架结构

### 4.1 TaskService 核心服务

`TaskService` 是任务系统的核心控制器，负责以下功能：

- **插件管理**：自动发现和注册任务处理插件
- **任务调度**：基于Cron的定时任务调度
- **任务执行**：异步任务执行和状态管理
- **生命周期管理**：任务创建、更新、查询

#### 4.1.1 关键组件

```java
@Service
public class TaskService implements InitializingBean {
    private final Map<TaskType, TaskProcessPlugin> pluginMap = new HashMap<>();
    private final TaskMapper taskMapper;
    private final ExecutorService executorService;
    private final List<TaskProcessPlugin> plugins;
}
```

#### 4.1.2 插件注册机制

```java
@Override
public void afterPropertiesSet() throws Exception {
    // 自动注册所有TaskProcessPlugin实现
    for (TaskProcessPlugin plugin : plugins) {
        if (plugin != null && plugin.getTaskType() != null) {
            pluginMap.put(plugin.getTaskType(), plugin);
            logger.info("📌 注册任务处理插件: " + plugin.getTaskType());
        }
    }
}
```

### 4.2 TaskProcessPlugin 插件接口

所有任务处理逻辑都通过插件实现，确保系统的可扩展性。

```java
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
```

#### 4.2.1 实现示例：ScrapingTaskProcessPlugin

```java
@Component
public class ScrapingTaskProcessPlugin implements TaskProcessPlugin {
    
    @Override
    public TaskResult handleTask(Task task) {
        try {
            // 解析任务参数
            TaskParameters params = new TaskParameters(task.getTaskParameters());
            String cik = params.getString("cik");
            String companyName = params.getString("companyName");
            
            // 执行数据抓取逻辑
            // ... scraping implementation
            
            return TaskResult.success("成功爬取数据");
        } catch (Exception e) {
            return TaskResult.failure("抓取失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public TaskType getTaskType() {
        return TaskType.SEC_SCRAPING;
    }
}
```

### 4.3 TaskResult 执行结果

```java
public class TaskResult {
    private boolean success;
    private String message;
    private Throwable error;
    
    public static TaskResult success(String message) {
        return new TaskResult(true, message, null);
    }
    
    public static TaskResult failure(String message) {
        return new TaskResult(false, message, null);
    }
    
    public static TaskResult failure(String message, Throwable error) {
        return new TaskResult(false, message, error);
    }
}
```

### 4.4 TaskParameters 参数管理

任务参数以JSON格式存储，提供类型安全的访问接口。

```java
public class TaskParameters {
    private final Map<String, Object> parameters;
    
    // 构造方法：从JSON字符串解析
    public TaskParameters(String json) { /* ... */ }
    
    // 类型安全的访问方法
    public String getString(String key) { /* ... */ }
    public Integer getInteger(String key) { /* ... */ }
    public Boolean getBoolean(String key) { /* ... */ }
    
    // 序列化为JSON
    public String toJson() { /* ... */ }
    
    // 静态工厂方法
    public static TaskParameters forScraping(String cik, String companyName) {
        return new TaskParameters()
            .put("cik", cik)
            .put("companyName", companyName);
    }
}
```

## 5. API接口

### 5.1 TaskController REST API

任务系统提供RESTful API用于任务管理和监控。

#### 5.1.1 获取任务统计信息

**接口**：`GET /api/tasks/stats`

**响应格式**：
```json
{
  "totalTasks": 150,
  "pendingTasks": 5,
  "runningTasks": 2,
  "completedTasks": 135,
  "failedTasks": 8,
  "retryTasks": 3
}
```

**实现**：
```java
@GetMapping("/stats")
public ResponseEntity<?> getTaskStats() {
    Map<String, Object> stats = new HashMap<>();
    stats.put("totalTasks", taskMapper.countAll());
    stats.put("pendingTasks", taskMapper.countByStatus("PENDING"));
    stats.put("completedTasks", taskMapper.countByStatus("COMPLETED"));
    // ...
    return ResponseEntity.ok(stats);
}
```

### 5.2 任务创建API (通过TaskService)

```java
// 创建数据抓取任务
String taskId = taskService.createTask(
    TaskType.SEC_SCRAPING,
    TaskParameters.forScraping("0001524258", "Alibaba Group").toJson()
);
```

### 5.3 任务查询API

```java
// 根据任务ID查询
Task task = taskService.getTaskStatus(taskId);

// 查询所有任务
List<Task> allTasks = taskService.getAllTasks();
```

## 6. 配置参数

### 6.1 Spring Boot 配置 (application.yml)

#### 6.1.1 数据库配置
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/sec13f?useSSL=false&serverTimezone=UTC&characterEncoding=utf8
    username: root
    password: "123456"
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

#### 6.1.2 MyBatis 配置
```yaml
mybatis:
  mapper-locations: classpath*:mapper/*.xml
  type-aliases-package: com.company.sec13f.repository.entity
  configuration:
    map-underscore-to-camel-case: true
    use-generated-keys: true
    default-fetch-size: 100
    default-statement-timeout: 30
```

#### 6.1.3 自定义任务配置
```yaml
sec-collector:
  database:
    max-connections: 10
  scraping:
    thread-pool-size: 3        # 任务执行线程池大小
    request-delay-ms: 100      # API请求间隔
    max-retries: 3             # 最大重试次数
  scheduling:
    enabled: true
    auto-scraping-cron: "0 0 2 * * ?"    # 每天凌晨2点执行
    retry-failed-cron: "0 0 */6 * * ?"   # 每6小时重试失败任务
```

### 6.2 任务调度配置

#### 6.2.1 定时任务调度
```java
@Scheduled(cron = "0 */1 * * * ?")  // 每分钟执行一次
public void scheduleTask() {
    // 获取待执行的任务 (PENDING状态)
    List<Task> pendingTasks = taskMapper.selectPendingTasks();
    
    // 获取需要重试的任务 (RETRY状态且时间已到期)
    List<Task> retryTasks = taskMapper.selectRetryTasksReadyForExecution(currentTime);
    
    // 异步执行所有任务
    pendingTasks.forEach(this::handleTask);
    retryTasks.forEach(this::handleTask);
}
```

#### 6.2.2 Cron 表达式说明

| 表达式 | 说明 | 用途 |
|--------|------|------|
| `0 */1 * * * ?` | 每分钟执行一次 | 任务调度检查 |
| `0 0 2 * * ?` | 每天凌晨2点执行 | 自动数据抓取 |
| `0 0 */6 * * ?` | 每6小时执行一次 | 重试失败任务 |

### 6.3 线程池配置

```java
private final ExecutorService executorService = Executors.newFixedThreadPool(3);
```

**配置参数**：
- **核心线程数**：3
- **任务队列**：无界队列
- **线程生存时间**：默认（长期存活）

## 7. 最佳实践

### 7.1 开发自定义任务插件

#### 7.1.1 插件开发步骤

1. **实现TaskProcessPlugin接口**：
```java
@Component
public class CustomTaskPlugin implements TaskProcessPlugin {
    
    @Override
    public TaskResult handleTask(Task task) {
        try {
            // 解析任务参数
            TaskParameters params = new TaskParameters(task.getTaskParameters());
            
            // 执行业务逻辑
            // ...
            
            return TaskResult.success("任务执行成功");
        } catch (Exception e) {
            logger.error("任务执行失败", e);
            return TaskResult.failure("任务执行失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public TaskType getTaskType() {
        return TaskType.CUSTOM_TASK; // 确保在TaskType枚举中定义
    }
}
```

2. **添加新的TaskType**：
```java
public enum TaskType {
    // 现有类型...
    CUSTOM_TASK,           // 自定义任务类型
    EMAIL_NOTIFICATION,    // 邮件通知任务
    DATA_BACKUP           // 数据备份任务
}
```

3. **注册插件**（自动注册）：
Spring会自动发现带有`@Component`注解的插件并注册到TaskService中。

#### 7.1.2 插件开发规范

- **异常处理**：必须捕获所有异常并返回适当的TaskResult
- **日志记录**：使用统一的Logger记录执行过程
- **参数验证**：验证任务参数的完整性和有效性
- **幂等性**：确保任务可以安全重复执行
- **资源管理**：正确管理外部资源（数据库连接、文件句柄等）

### 7.2 任务参数设计建议

#### 7.2.1 参数结构设计

```json
{
  "type": "scraping",
  "source": {
    "cik": "0001524258",
    "companyName": "Alibaba Group"
  },
  "options": {
    "includeHoldings": true,
    "maxRetries": 3,
    "batchSize": 100
  },
  "metadata": {
    "requestedBy": "admin",
    "priority": "high"
  }
}
```

#### 7.2.2 参数验证

```java
public TaskResult handleTask(Task task) {
    TaskParameters params = new TaskParameters(task.getTaskParameters());
    
    // 必需参数验证
    String cik = params.getString("cik");
    if (cik == null || cik.trim().isEmpty()) {
        return TaskResult.failure("缺少必需参数：cik");
    }
    
    // 参数格式验证
    if (!isValidCik(cik)) {
        return TaskResult.failure("无效的CIK格式：" + cik);
    }
    
    // 继续执行任务...
}
```

### 7.3 错误处理和日志记录

#### 7.3.1 分层错误处理

```java
public TaskResult handleTask(Task task) {
    try {
        // 业务逻辑执行
        return executeBusinessLogic(task);
        
    } catch (ValidationException e) {
        // 参数验证错误 - 不重试
        logger.warn("任务参数验证失败: " + task.getTaskId(), e);
        return TaskResult.failure("参数验证失败: " + e.getMessage());
        
    } catch (NetworkException e) {
        // 网络错误 - 可重试
        logger.error("网络连接失败: " + task.getTaskId(), e);
        return TaskResult.failure("网络连接失败，将自动重试: " + e.getMessage(), e);
        
    } catch (Exception e) {
        // 未知错误 - 记录详细信息
        logger.error("任务执行异常: " + task.getTaskId(), e);
        return TaskResult.failure("任务执行异常: " + e.getMessage(), e);
    }
}
```

#### 7.3.2 结构化日志记录

```java
// 任务开始
logger.info("🚀 开始执行任务: " + task.getTaskId() + " [" + task.getTaskType() + "]");

// 关键步骤
logger.info("📥 正在处理公司: " + companyName + " (CIK: " + cik + ")");

// 进度更新
logger.info("📊 已处理 " + processedCount + "/" + totalCount + " 条记录");

// 任务完成
logger.info("✅ 任务完成: " + task.getTaskId() + " - " + resultMessage);

// 任务失败
logger.error("❌ 任务失败: " + task.getTaskId() + " - " + errorMessage);
```

### 7.4 性能优化建议

#### 7.4.1 批量操作

```java
// 批量插入而非逐条插入
List<Filing> filings = new ArrayList<>();
for (Filing filing : scrapedFilings) {
    filings.add(filing);
    if (filings.size() >= BATCH_SIZE) {
        filingMapper.insertBatch(filings);
        filings.clear();
    }
}
// 处理剩余记录
if (!filings.isEmpty()) {
    filingMapper.insertBatch(filings);
}
```

#### 7.4.2 连接池优化

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20        # 根据并发任务数调整
      minimum-idle: 5              # 保持最小连接数
      connection-timeout: 30000    # 连接超时时间
      idle-timeout: 600000         # 空闲连接超时
      max-lifetime: 1800000        # 连接最大生存时间
      leak-detection-threshold: 60000  # 连接泄漏检测
```

#### 7.4.3 任务分片

对于大型任务，建议分解为多个小任务：

```java
public List<String> createScrapingTasks(List<String> cikList) {
    List<String> taskIds = new ArrayList<>();
    
    // 将大型CIK列表分片处理
    int batchSize = 10;
    for (int i = 0; i < cikList.size(); i += batchSize) {
        List<String> batch = cikList.subList(i, Math.min(i + batchSize, cikList.size()));
        
        TaskParameters params = new TaskParameters()
            .put("cikList", batch)
            .put("batchIndex", i / batchSize);
            
        String taskId = taskService.createTask(TaskType.SEC_SCRAPING, params.toJson());
        taskIds.add(taskId);
    }
    
    return taskIds;
}
```

### 7.5 监控和告警

#### 7.5.1 任务监控指标

- **执行成功率**：成功任务数 / 总任务数
- **平均执行时间**：任务执行时间统计
- **重试率**：重试任务数 / 失败任务数
- **队列长度**：待执行任务数量

#### 7.5.2 告警条件

```java
// 示例：检查任务健康状态
public TaskHealthStatus checkTaskHealth() {
    long pendingTasks = taskMapper.countByStatus("PENDING");
    long failedTasks = taskMapper.countByStatus("FAILED");
    long totalTasks = taskMapper.countAll();
    
    TaskHealthStatus status = new TaskHealthStatus();
    
    // 待处理任务过多告警
    if (pendingTasks > 100) {
        status.addWarning("待处理任务过多: " + pendingTasks);
    }
    
    // 失败率过高告警
    double failureRate = (double) failedTasks / totalTasks;
    if (failureRate > 0.1) { // 失败率超过10%
        status.addAlert("任务失败率过高: " + String.format("%.2f%%", failureRate * 100));
    }
    
    return status;
}
```

---

## 总结

SEC Data Collector 任务系统提供了一个完整、可扩展的异步任务处理解决方案。通过插件化架构、智能重试机制和完善的状态管理，该系统能够可靠地处理各种长时间运行的数据处理任务。

开发者可以通过实现 `TaskProcessPlugin` 接口来扩展新的任务类型，系统会自动发现和注册这些插件。配合完善的配置管理和监控机制，该任务系统为SEC数据收集和分析提供了稳定可靠的基础架构。
