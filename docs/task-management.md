# 任务管理系统设计

## 概述

本项目采用基于数据库的任务驱动架构，通过`scraping_tasks`表管理所有SEC 13F数据抓取任务的生命周期。该系统支持异步任务执行、状态跟踪、失败重试和任务持久化。

## 数据库模型结构

### scraping_tasks表结构

```sql
CREATE TABLE IF NOT EXISTS scraping_tasks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,     -- 主键ID
    task_id TEXT NOT NULL UNIQUE,            -- 唯一任务标识符
    cik TEXT NOT NULL,                       -- SEC公司识别代码
    company_name TEXT NOT NULL,              -- 公司名称
    status TEXT NOT NULL,                    -- 任务状态
    message TEXT,                           -- 状态消息
    error_message TEXT,                     -- 错误信息
    start_time TIMESTAMP,                   -- 任务开始时间
    end_time TIMESTAMP,                     -- 任务结束时间
    saved_filings INTEGER DEFAULT 0,       -- 已保存的文件数量
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP   -- 更新时间
);
```

### 字段说明

| 字段名 | 类型 | 说明 | 示例 |
|--------|------|------|------|
| `id` | INTEGER | 数据库自增主键 | 1, 2, 3... |
| `task_id` | TEXT | 唯一任务标识符，格式为"scrape_{cik}_{timestamp}" | "scrape_0001067983_20250825014925" |
| `cik` | TEXT | SEC公司识别代码，10位数字格式 | "0001067983" |
| `company_name` | TEXT | 公司名称 | "Berkshire Hathaway Inc" |
| `status` | TEXT | 任务状态枚举值 | "PENDING", "RUNNING", "COMPLETED", "FAILED" |
| `message` | TEXT | 当前任务状态的描述信息 | "找到 44 个13F文件", "已保存 5/10 个文件" |
| `error_message` | TEXT | 错误详情（仅在失败时填充） | "SEC request failed with status: 503" |
| `start_time` | TIMESTAMP | 任务实际开始执行的时间 | "2025-08-25 01:49:25.855" |
| `end_time` | TIMESTAMP | 任务完成或失败的时间 | "2025-08-25 01:52:30.123" |
| `saved_filings` | INTEGER | 成功保存到数据库的文件数量 | 5 |
| `created_at` | TIMESTAMP | 任务创建时间（系统自动设置） | "2025-08-25 01:49:20.000" |
| `updated_at` | TIMESTAMP | 最后更新时间（系统自动维护） | "2025-08-25 01:52:30.123" |

## 任务状态管理

### 状态枚举 (TaskStatus)

```java
public enum TaskStatus {
    PENDING,    // 等待执行
    RUNNING,    // 正在执行
    COMPLETED,  // 执行成功
    FAILED      // 执行失败
}
```

### 状态转换流程

```
PENDING → RUNNING → COMPLETED  (成功路径)
          ↓
        FAILED                 (失败路径)
```

### 状态详细说明

1. **PENDING（等待）**
   - 任务刚创建，等待执行
   - message: "任务已创建"
   - 此状态下任务在队列中等待线程池调度

2. **RUNNING（运行中）**
   - 任务正在执行中
   - start_time被设置
   - message会动态更新进度，如："找到 44 个13F文件"、"已保存 3/10 个文件"

3. **COMPLETED（已完成）**
   - 任务成功执行完毕
   - end_time被设置
   - saved_filings记录实际保存的文件数量
   - message: "任务完成，共保存 X 个文件"

4. **FAILED（失败）**
   - 任务执行过程中出现异常
   - end_time被设置
   - error_message记录具体错误信息
   - message描述失败原因

## 任务框架架构

### 核心组件

#### 1. DataScrapingService
- **职责**: 任务执行引擎，负责实际的数据抓取工作
- **特性**:
  - 使用CompletableFuture实现异步执行
  - 线程池大小限制为3，遵守SEC API频率限制
  - 内存中维护任务状态缓存(`ConcurrentHashMap`)
  - 与数据库同步任务状态

#### 2. ScheduledScrapingService
- **职责**: 定时任务调度器，基于Spring Scheduler框架
- **调度策略**:
  - **每日数据收集**: `@Scheduled(cron = "0 0 2 * * ?")` - 每天凌晨2点执行
  - **失败重试**: `@Scheduled(fixedRate = 3600000)` - 每小时重试失败的任务
- **重试机制**: 使用Spring Retry，最大重试3次，指数退避策略

#### 3. TaskDAO
- **职责**: 数据访问层，负责任务的数据库持久化
- **核心方法**:
  - `saveTask()`: 保存新任务
  - `updateTask()`: 更新任务状态
  - `getAllTasks()`: 获取所有任务
  - `getTaskById()`: 根据ID获取任务

### 任务生命周期管理

#### 1. 任务创建
```java
// 生成唯一任务ID
String taskId = generateTaskId(cik); // 格式: "scrape_{cik}_{timestamp}"

// 创建任务状态对象
ScrapingStatus status = new ScrapingStatus(taskId, cik, companyName);
status.setStatus(TaskStatus.PENDING);
status.setMessage("任务已创建");

// 持久化到数据库
taskDAO.saveTask(status);
```

#### 2. 任务执行
```java
CompletableFuture.supplyAsync(() -> {
    // 更新状态为运行中
    status.setStatus(TaskStatus.RUNNING);
    status.setStartTime(LocalDateTime.now());
    taskDAO.updateTask(status);
    
    // 执行实际业务逻辑
    List<Filing> filings = scraper.getCompanyFilings(cik);
    
    // 动态更新进度
    status.setMessage("找到 " + filings.size() + " 个13F文件");
    taskDAO.updateTask(status);
    
    // ... 处理文件 ...
    
}, executorService);
```

#### 3. 任务完成处理
```java
// 成功完成
status.setStatus(TaskStatus.COMPLETED);
status.setEndTime(LocalDateTime.now());
status.setSavedFilings(savedCount);
status.setMessage("任务完成，共保存 " + savedCount + " 个文件");

// 异常处理
} catch (Exception e) {
    status.setStatus(TaskStatus.FAILED);
    status.setEndTime(LocalDateTime.now());
    status.setError(e.getMessage());
    status.setMessage("任务失败: " + e.getMessage());
}
taskDAO.updateTask(status);
```

### 服务重启恢复机制

系统启动时会自动恢复中断的任务：

```java
private void loadExistingTasks() throws SQLException {
    List<ScrapingStatus> existingTasks = taskDAO.getAllTasks();
    for (ScrapingStatus task : existingTasks) {
        if (task.getStatus() == TaskStatus.RUNNING) {
            // 将因服务重启中断的任务标记为失败
            task.setStatus(TaskStatus.FAILED);
            task.setEndTime(LocalDateTime.now());
            task.setMessage("任务因服务重启而中断");
            task.setError("Service restart interrupted task");
            taskDAO.updateTask(task);
        }
    }
}
```

## API接口

### REST API端点

- `POST /api/scraping/start` - 启动新的抓取任务
- `GET /api/scraping/status/{taskId}` - 查询任务状态
- `GET /api/scraping/tasks` - 获取所有任务列表
- `POST /api/scheduling/trigger-collection` - 手动触发数据收集
- `POST /api/scheduling/trigger-retry` - 手动触发失败重试

### 使用示例

```bash
# 启动抓取任务
curl -X POST "http://localhost:8080/api/scraping/start" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "cik=0001067983&companyName=Berkshire+Hathaway+Inc"

# 查询任务状态
curl "http://localhost:8080/api/scraping/status/scrape_0001067983_20250825014925"

# 获取所有任务
curl "http://localhost:8080/api/scraping/tasks"
```

## 监控和维护

### 任务监控指标

1. **执行时间**: 通过`getDurationSeconds()`方法计算任务执行耗时
2. **成功率**: 统计COMPLETED vs FAILED的比例
3. **数据产出**: 通过`saved_filings`字段跟踪实际保存的数据量
4. **系统负载**: 监控线程池使用情况和内存中任务缓存大小

### 数据清理策略

```sql
-- 清理已完成的历史任务（可根据需要定期执行）
DELETE FROM scraping_tasks WHERE status = 'COMPLETED' AND created_at < datetime('now', '-30 days');

-- 清理失败且不再需要的任务
DELETE FROM scraping_tasks WHERE status = 'FAILED' AND created_at < datetime('now', '-7 days');
```

### 故障排查

1. **任务卡在RUNNING状态**: 检查线程池是否死锁，服务是否异常终止
2. **频繁失败**: 检查SEC API连接、网络状况、频率限制
3. **数据不一致**: 对比内存缓存和数据库状态，必要时重启服务恢复

## 配置参数

### 线程池配置
- **核心线程数**: 3（遵守SEC API每秒10次请求限制）
- **任务队列**: 无界队列，防止任务丢失

### 调度配置
- **每日收集时间**: 凌晨2:00（避开业务高峰期）
- **重试间隔**: 每小时检查一次失败任务
- **公司间延迟**: 5秒（减少API压力）

### 重试策略
- **最大重试次数**: 3次
- **退避策略**: 指数退避，初始延迟2秒，倍数2.0
- **重试条件**: 网络异常、临时性API错误

## 最佳实践

1. **任务ID设计**: 使用时间戳确保唯一性，便于排查问题
2. **状态更新频率**: 在关键节点及时更新状态，便于监控
3. **错误信息**: 记录详细的错误堆栈，方便故障诊断
4. **资源管理**: 及时关闭HTTP连接，避免资源泄漏
5. **数据一致性**: 数据库事务确保filing和holdings的原子性保存