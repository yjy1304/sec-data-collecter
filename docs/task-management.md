# 任务管理系统设计

## 概述

本项目采用基于数据库的任务驱动架构，通过`tasks`表管理所有任务的生命周期。该系统支持异步任务执行、状态跟踪、失败重试和任务持久化。

## 数据库模型结构

### tasks表结构

```sql
CREATE TABLE IF NOT EXISTS tasks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,     -- 主键ID
    task_id TEXT,                           -- 当前任务id
    task_type TEXT NOT NULL,              -- 任务类型
    status TEXT NOT NULL,                    -- 任务状态
    message TEXT,                           -- 当前任务执行结果信息
    retry_times INTEGER,                   -- 重试次数
    start_time TIMESTAMP,                   -- 任务开始时间
    next_execute_time TIMESTAMP,                   -- 下次执行时间
    end_time TIMESTAMP,                     -- 任务结束时间
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP   -- 更新时间
);
```

### 字段说明

| 字段名                 | 类型 | 说明                   | 示例                                          |
|---------------------|------|----------------------|---------------------------------------------|
| `id`                | INTEGER | 数据库自增主键              | 1, 2, 3...                                  |
| `task_id`            | TEXT | 当前任务id,一般使用UUID      | "123456"                                    |
| `status`            | TEXT | 任务状态枚举值,参考TaskStatus | "PENDING", "RUNNING", "COMPLETED", "FAILED" |
| `task_type`         | TEXT | 任务类型枚举值,参考TaskType   | "SCRAP_HOLDING", "SCRAP_FINANCIAL_REPORT"   |
| `message`           | TEXT | 当前任务执行结果信息           | "找到 44 个13F文件", "已保存 5/10 个文件"              |
| `retry_times`       | INTEGER | 重试次数                 | 0, 1, 2                                     |
| `next_execute_time` | TIMESTAMP | 下次执行时间               | "2025-08-25 01:49:25.855"                   |
| `start_time`        | TIMESTAMP | 任务实际开始执行的时间          | "2025-08-25 01:49:25.855"                   |
| `end_time`          | TIMESTAMP | 任务完成或失败的时间           | "2025-08-25 01:52:30.123"                   |
| `created_at`        | TIMESTAMP | 任务创建时间（系统自动设置）       | "2025-08-25 01:49:20.000"                   |
| `updated_at`        | TIMESTAMP | 最后更新时间（系统自动维护）       | "2025-08-25 01:52:30.123"                   |

## 任务状态管理

### 状态枚举 (TaskStatus)

```java
public enum TaskStatus {
    PENDING,    // 等待执行
    RETRY,    // 正在执行
    COMPLETED,  // 执行成功
    FAILED      // 执行失败
}

public enum TaskType {
    SCRAP_HOLDING,    // 抓取持仓
    SCRAP_FINANCIAL_REPORT    // 抓取财报
}
```

### 状态转换流程


### 状态详细说明

1. **PENDING（等待）**
   - 任务刚创建，等待执行
   - message: "任务已创建"
   - retry_times置为初始值0
   - 此状态下任务在队列中等待TaskService.handleTask(ScrapingTask task)调度执行

2. **RETRY（等待重试）**
   - 任务执行失败了，需要重试
   - retry_times被设置+1
   - message会上一次失败信息，比如"访问sec服务失败"

3. **COMPLETED（已完成）**
   - 任务成功执行完毕
   - end_time被设置
   - message: "任务完成，共保存 X 个文件"

4. **FAILED（失败）**
   - 任务执行过程中出现并且异常超过重试次数
   - end_time被设置
   - message记录具体错误信息

## 任务框架架构

### 核心组件

#### 1. TaskService
- **职责**: 任务服务，负责任务的调度执行，调用不同taskType对应的任务插件执行任务并会写执行结果
- **核心方法**:
    - `handleTask(TaskDO taskDO)`: 根据任务类型获取TaskProcessPlugin的实现类并执行任务
    - `scheduleTask(TaskDO taskDO)`: 根据任务状态PENDING/RETRY状态和next_execute_time捞取到期待并调用handleTask执行任务
- **调度策略**:
    - **每日数据收集**: `@Scheduled(cron = "0 10/* * * * ?")` - 对于scheduleTask()注解该任务，每10分钟执行一次


#### 2. TaskProcessPlugin
- **职责**: 任务服务，负责任务的调度执行，调用不同taskType对应的任务插件执行任务并会写执行结果
- **核心方法**:
    - `TaskResult handleTask()`: 执行任务
    - `TaskType taskType()`: 返回任务类型

#### 2. ScheduledScrapingService
- **职责**: 定时任务调度器，基于Spring Scheduler框架


#### 3. TaskDAO
- **职责**: 数据访问层，负责任务的数据库持久化
- **核心方法**:
  - `saveTask()`: 保存新任务
  - `updateTask()`: 更新任务状态
  - `getAllTasks()`: 获取所有任务
  - `getTaskById()`: 根据ID获取任务

## API接口

### REST API端点
- `GET /api/tasks/status/{taskId}` - 查询任务状态
- `GET /api/tasks` - 获取所有任务列表


### 使用示例

```bash

# 查询任务状态
curl "http://localhost:8080/api/scraping/status/scrape_0001067983_20250825014925"

# 获取所有任务
curl "http://localhost:8080/api/scraping/tasks"
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
- **每日收集时间**: 每10分钟调度一次
- **重试间隔**: 目前写死为1小时
- **公司间延迟**: 5秒（减少API压力）

### 重试策略
- **最大重试次数**: 3次
- **退避策略**: 指数退避，初始延迟2秒，倍数2.0
- **重试条件**: 网络异常、临时性API错误

## 最佳实践

1. **任务ID设计**: 使用UUID即可
2. **状态更新频率**: 在关键节点及时更新状态，便于监控
3. **错误信息**: 记录详细的错误堆栈，方便故障诊断
4. **资源管理**: 及时关闭HTTP连接，避免资源泄漏
5. **数据一致性**: 数据库事务确保filing和holdings的原子性保存