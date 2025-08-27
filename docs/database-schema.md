# 数据库设计文档

## 🗄️ 数据库概览

SEC 13F Parser使用SQLite作为数据存储，采用MyBatis ORM框架进行数据访问。数据库设计遵循关系型数据库的规范化原则，确保数据完整性和查询效率。

## 📋 表结构设计

### 1. filings表 - 13F文件元数据

```sql
CREATE TABLE IF NOT EXISTS filings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    cik TEXT NOT NULL,                    -- SEC Central Index Key
    company_name TEXT NOT NULL,           -- 机构名称
    filing_type TEXT NOT NULL,            -- 文件类型 (13F-HR)
    filing_date DATE NOT NULL,            -- 报告日期
    accession_number TEXT NOT NULL,       -- SEC文件编号
    form_file TEXT NOT NULL,              -- 原始文件名
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- 更新时间
    UNIQUE(accession_number, form_file)   -- 防重复约束
);
```

**字段详细说明:**

| 字段名 | 数据类型 | 约束 | 说明 | 示例值 |
|--------|----------|-----|------|--------|
| `id` | INTEGER | PRIMARY KEY, AUTO_INCREMENT | 数据库主键 | 1, 2, 3... |
| `cik` | TEXT | NOT NULL | SEC公司识别码，10位数字字符串 | "0001067983" |
| `company_name` | TEXT | NOT NULL | 机构完整名称 | "Berkshire Hathaway Inc" |
| `filing_type` | TEXT | NOT NULL | 文件类型，通常为"13F-HR"或"13F-HR/A" | "13F-HR" |
| `filing_date` | DATE | NOT NULL | 13F报告的截止日期 | "2023-12-31" |
| `accession_number` | TEXT | NOT NULL | SEC分配的唯一文件编号 | "0000950123-25-008361" |
| `form_file` | TEXT | NOT NULL | 在SEC服务器上的原始文件名 | "0000950123-25-008361.txt" |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 记录创建时间 | "2025-01-15 10:30:25" |
| `updated_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 记录最后更新时间 | "2025-01-15 10:30:25" |

**索引设计:**
```sql
-- 复合唯一索引，防止重复数据
CREATE UNIQUE INDEX idx_filings_accession_form ON filings(accession_number, form_file);

-- CIK查询优化索引
CREATE INDEX idx_filings_cik ON filings(cik);

-- 日期范围查询索引
CREATE INDEX idx_filings_date ON filings(filing_date);
```

### 2. holdings表 - 持仓明细数据

```sql
CREATE TABLE IF NOT EXISTS holdings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    filing_id INTEGER NOT NULL,           -- 外键关联filings.id
    name_of_issuer TEXT NOT NULL,         -- 发行人名称
    cusip TEXT NOT NULL,                  -- CUSIP标识符
    value DECIMAL(15,2),                  -- 持仓市值(千美元)
    shares BIGINT,                        -- 持股数量
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- 更新时间
    FOREIGN KEY (filing_id) REFERENCES filings (id) ON DELETE CASCADE
);
```

**字段详细说明:**

| 字段名 | 数据类型 | 约束 | 说明 | 示例值 |
|--------|----------|-----|------|--------|
| `id` | INTEGER | PRIMARY KEY, AUTO_INCREMENT | 数据库主键 | 1, 2, 3... |
| `filing_id` | INTEGER | NOT NULL, FOREIGN KEY | 关联的13F文件ID | 1 |
| `name_of_issuer` | TEXT | NOT NULL | 股票发行公司名称 | "Apple Inc" |
| `cusip` | TEXT | NOT NULL | 9位CUSIP标识符 | "037833100" |
| `value` | DECIMAL(15,2) | NULL允许 | 持仓市值，单位千美元 | 1000000000.00 |
| `shares` | BIGINT | NULL允许 | 持有股票数量 | 5000000 |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 记录创建时间 | "2025-01-15 10:30:25" |
| `updated_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 记录最后更新时间 | "2025-01-15 10:30:25" |

**索引设计:**
```sql
-- 文件ID外键索引
CREATE INDEX idx_holdings_filing_id ON holdings(filing_id);

-- CUSIP查询优化索引
CREATE INDEX idx_holdings_cusip ON holdings(cusip);

-- 市值排序索引
CREATE INDEX idx_holdings_value ON holdings(value DESC);

-- 复合查询索引
CREATE INDEX idx_holdings_filing_cusip ON holdings(filing_id, cusip);
```

### 3. scraping_tasks表 - 任务管理

```sql
CREATE TABLE IF NOT EXISTS scraping_tasks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id TEXT NOT NULL UNIQUE,         -- 唯一任务标识符
    cik TEXT NOT NULL,                    -- 目标机构CIK
    company_name TEXT NOT NULL,           -- 机构名称
    status TEXT NOT NULL,                 -- 任务状态枚举
    message TEXT,                         -- 状态消息
    error_message TEXT,                   -- 错误详情
    start_time TIMESTAMP,                 -- 开始执行时间
    end_time TIMESTAMP,                   -- 结束时间
    saved_filings INTEGER DEFAULT 0,      -- 成功保存文件数
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP   -- 更新时间
);
```

**字段详细说明:**

| 字段名 | 数据类型 | 约束 | 说明 | 示例值 |
|--------|----------|-----|------|--------|
| `id` | INTEGER | PRIMARY KEY, AUTO_INCREMENT | 数据库主键 | 1, 2, 3... |
| `task_id` | TEXT | NOT NULL, UNIQUE | 格式: "scrape_{cik}_{timestamp}" | "scrape_0001067983_1674648565123" |
| `cik` | TEXT | NOT NULL | 目标机构的CIK | "0001067983" |
| `company_name` | TEXT | NOT NULL | 目标机构名称 | "Berkshire Hathaway Inc" |
| `status` | TEXT | NOT NULL | 枚举: PENDING/RUNNING/COMPLETED/FAILED | "COMPLETED" |
| `message` | TEXT | NULL允许 | 当前状态描述 | "任务完成，共保存 5 个文件" |
| `error_message` | TEXT | NULL允许 | 错误详细信息 | "SEC request failed: 503" |
| `start_time` | TIMESTAMP | NULL允许 | 任务开始执行时间 | "2025-01-15 10:30:25" |
| `end_time` | TIMESTAMP | NULL允许 | 任务完成时间 | "2025-01-15 10:35:45" |
| `saved_filings` | INTEGER | DEFAULT 0 | 成功保存的文件数量 | 5 |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 任务创建时间 | "2025-01-15 10:30:20" |
| `updated_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 最后更新时间 | "2025-01-15 10:35:45" |

**任务状态枚举:**
- `PENDING`: 等待执行
- `RUNNING`: 正在执行  
- `COMPLETED`: 成功完成
- `FAILED`: 执行失败

**索引设计:**
```sql
-- 任务ID唯一索引
CREATE UNIQUE INDEX idx_tasks_task_id ON scraping_tasks(task_id);

-- 状态查询索引
CREATE INDEX idx_tasks_status ON scraping_tasks(status);

-- CIK查询索引
CREATE INDEX idx_tasks_cik ON scraping_tasks(cik);

-- 时间范围查询索引
CREATE INDEX idx_tasks_created_at ON scraping_tasks(created_at);
```

## 🔗 表关系设计

### ER关系图

```
┌─────────────┐       ┌──────────────┐       ┌──────────────┐
│   filings   │ 1   * │   holdings   │       │scraping_tasks│
│             │───────│              │       │   (独立表)   │
│ id (PK)     │       │ id (PK)      │       │              │
│ cik         │       │ filing_id(FK)│       │ id (PK)      │
│ company_name│       │ name_of_issuer│       │ task_id      │
│ filing_type │       │ cusip        │       │ cik          │
│ filing_date │       │ value        │       │ status       │
│ ...         │       │ shares       │       │ ...          │
└─────────────┘       └──────────────┘       └──────────────┘
```

### 关系约束

1. **filings ↔ holdings (1:N)**
   - 一个13F文件包含多个持仓记录
   - 外键约束: `holdings.filing_id → filings.id`
   - 级联删除: 删除filing时自动删除相关holdings

2. **scraping_tasks (独立表)**
   - 任务管理表独立存在
   - 不与其他表建立外键关系
   - 通过CIK字段进行逻辑关联

## 🔍 查询模式优化

### 常用查询场景

1. **按机构查询所有13F文件**
```sql
SELECT * FROM filings WHERE cik = ? ORDER BY filing_date DESC;
```

2. **查询机构最新持仓**
```sql
SELECT h.* FROM holdings h
JOIN filings f ON h.filing_id = f.id
WHERE f.cik = ? AND f.filing_date = (
    SELECT MAX(filing_date) FROM filings WHERE cik = ?
)
ORDER BY h.value DESC;
```

3. **统计机构持仓总市值**
```sql
SELECT f.filing_date, SUM(h.value) as total_value
FROM filings f
JOIN holdings h ON f.id = h.filing_id
WHERE f.cik = ?
GROUP BY f.filing_date
ORDER BY f.filing_date;
```

4. **查询特定股票的机构持有趋势**
```sql
SELECT f.filing_date, f.company_name, h.shares, h.value
FROM holdings h
JOIN filings f ON h.filing_id = f.id
WHERE h.cusip = ?
ORDER BY f.filing_date DESC;
```

### 性能优化建议

1. **索引策略**
   - 主要查询字段建立单列索引
   - 复合查询条件建立组合索引
   - 避免过多索引影响写入性能

2. **查询优化**
   - 使用LIMIT限制返回结果数量
   - WHERE条件中优先使用索引字段
   - 避免SELECT *，只查询需要的字段

3. **批量操作**
   - 使用MyBatis批量插入优化大量数据写入
   - 事务边界合理控制，避免长事务

## 🎯 数据完整性

### 约束设计

1. **主键约束**
   - 每个表都有自增主键id
   - 确保记录唯一性

2. **外键约束**
   - holdings.filing_id → filings.id
   - 级联删除保证数据一致性

3. **唯一性约束**
   - filings: (accession_number, form_file)
   - scraping_tasks: task_id

4. **非空约束**
   - 关键业务字段设置NOT NULL
   - 防止关键数据缺失

### 数据验证

1. **应用层验证**
   - CIK格式验证（10位数字）
   - CUSIP格式验证（9位字符）
   - 日期格式和范围验证

2. **数据库层验证**
   - CHECK约束（如果需要）
   - 触发器验证（复杂业务规则）

## 🚀 扩展性设计

### 水平扩展

1. **分区策略**
   - 按时间分区（filing_date）
   - 按机构分区（cik hash）

2. **读写分离**
   - 主库负责写操作
   - 从库负责读查询

### 垂直扩展

1. **表拆分**
   - 冷热数据分离
   - 大字段独立存储

2. **索引优化**
   - 部分索引（条件索引）
   - 覆盖索引减少回表

## 📊 数据字典

### 枚举值定义

**filing_type枚举:**
- `13F-HR`: 初始13F报告
- `13F-HR/A`: 修正后的13F报告

**task_status枚举:**
- `PENDING`: 任务创建但未开始
- `RUNNING`: 任务执行中
- `COMPLETED`: 任务成功完成
- `FAILED`: 任务执行失败

### 数据范围

**数值字段范围:**
- `holdings.value`: 0 ~ 999,999,999,999,999.99 (千美元)
- `holdings.shares`: 0 ~ 9,223,372,036,854,775,807 (股数)
- `scraping_tasks.saved_filings`: 0 ~ 2,147,483,647 (文件数)

**文本字段长度:**
- `cik`: 固定10位数字字符串
- `cusip`: 固定9位字符串
- `company_name`: 建议最大255字符
- `accession_number`: 标准SEC格式，约20字符

## 🔧 维护操作

### 数据清理

1. **清理过期任务**
```sql
DELETE FROM scraping_tasks 
WHERE status = 'COMPLETED' 
AND created_at < datetime('now', '-30 days');
```

2. **清理失败任务**
```sql
DELETE FROM scraping_tasks 
WHERE status = 'FAILED' 
AND created_at < datetime('now', '-7 days');
```

### 备份恢复

1. **SQLite备份**
```bash
# 创建备份
sqlite3 sec13f.db ".backup backup_$(date +%Y%m%d).db"

# 恢复备份
sqlite3 sec13f_restored.db ".restore backup_20250115.db"
```

2. **数据导出**
```bash
# 导出为SQL
sqlite3 sec13f.db .dump > backup.sql

# 导出为CSV
sqlite3 -header -csv sec13f.db "select * from filings;" > filings.csv
```