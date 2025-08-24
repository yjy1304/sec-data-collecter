# SEC 13F Parser - 机构持仓分析系统

这是一个全面的SEC 13F文件解析和机构持仓分析系统，提供Web界面供用户查询和分析投资机构的持仓信息。

## 功能特点

### 核心功能
- **真实数据爬取**: 从SEC EDGAR官方API获取真实的13F持仓数据
- **智能数据验证**: 自动验证和清理数据，确保数据质量
- **持仓数据存储**: 使用SQLite数据库存储机构持仓信息
- **投资组合分析**: 提供详细的投资组合分析和趋势分析
- **Web界面**: 现代化的Web界面用于数据查询和可视化
- **爬取任务管理**: 可视化的任务管理和进度监控系统

### 分析功能
- **机构概览**: 显示机构基本信息和文件数量
- **重仓持股**: 按市值排序的主要持仓
- **投资组合摘要**: 包括持股数量、总市值、行业分布
- **持仓变化**: 对比不同期间的持仓变化
- **趋势分析**: 特定股票的历史持仓趋势

## 系统架构

### 后端技术栈
- **Java 8**: 核心开发语言
- **Maven**: 依赖管理和构建工具
- **Jetty**: 嵌入式Web服务器
- **SQLite**: 轻量级数据库
- **Jackson**: JSON序列化和反序列化

### 主要组件
- `RealSECScraper`: 真实数据爬取引擎
- `DataScrapingService`: 爬取任务管理服务
- `HoldingAnalysisService`: 核心分析服务
- `DataValidator`: 数据验证和清理工具
- `AnalysisServlet`: 分析API接口
- `ScrapingServlet`: 爬取管理API接口
- `FilingDAO`: 数据访问层
- `WebServer`: 嵌入式Web服务器

## 快速开始

### 环境要求
- Java 8 或更高版本
- Maven 3.6 或更高版本

### 构建和运行

1. **编译项目**:
   ```bash
   mvn clean compile
   ```

2. **打包项目**:
   ```bash
   mvn package -DskipTests
   ```

3. **运行服务器**:
   ```bash
   java -jar target/sec-13f-parser-1.0.0.jar
   ```

4. **访问应用**:
   - 主页: `http://localhost:8080`
   - 高级分析: `http://localhost:8080/analysis.html`
   - 数据爬取: `http://localhost:8080/scraping.html`

## API接口文档

### 基础搜索API

#### 搜索机构持仓
```
GET /search?cik=0001524258
```

### 分析API

#### 机构概览分析
```
GET /api/analysis/overview?cik=0001524258
```

#### 重仓持股分析
```
GET /api/analysis/top-holdings?cik=0001524258&limit=20
```

#### 投资组合摘要
```
GET /api/analysis/portfolio-summary?cik=0001524258
```

#### 持仓变化分析
```
GET /api/analysis/holding-changes?cik=0001524258
```

#### 特定股票趋势分析
```
GET /api/analysis/holding-trends?cik=0001524258&cusip=037833100
```

### 数据爬取API

#### 爬取单个机构数据
```
POST /api/scraping/scrape
Body: cik=0001524258&companyName=Alibaba Group Holding Limited
```

#### 爬取最新13F文件
```
POST /api/scraping/scrape-latest
Body: cik=0001524258&companyName=Alibaba Group Holding Limited
```

#### 批量爬取机构数据
```
POST /api/scraping/scrape-batch
```

#### 查询爬取任务状态
```
GET /api/scraping/status?taskId=scrape_0001524258_1692308734567
GET /api/scraping/tasks
```

### 响应示例

#### 机构概览响应
```json
{
  "institution": {
    "cik": "0001524258",
    "name": "Alibaba Group Holding Limited",
    "totalFilings": 1
  },
  "portfolioSummary": {
    "asOfDate": [2023, 11, 15],
    "numberOfHoldings": 20,
    "totalValue": 20890000,
    "totalShares": 20170000,
    "sectorAllocation": {
      "Technology": 28.25,
      "Other": 71.75
    }
  },
  "topHoldings": [...]
}
```

## 数据库结构

### filings表
- `id`: 自增主键
- `cik`: 机构CIK编号
- `company_name`: 机构名称
- `filing_type`: 文件类型 (13F)
- `filing_date`: 报告日期
- `accession_number`: SEC文件编号

### holdings表
- `id`: 自增主键
- `filing_id`: 关联的文件ID
- `name_of_issuer`: 发行人名称
- `cusip`: CUSIP标识符
- `value`: 持仓市值 (千美元)
- `shares`: 持股数量

## 使用示例

### 查询阿里巴巴的持仓信息
```bash
curl --noproxy "*" "http://localhost:8080/api/analysis/overview?cik=0001524258"
```

### 查询重仓持股
```bash
curl --noproxy "*" "http://localhost:8080/api/analysis/top-holdings?cik=0001524258&limit=10"
```

## 前端界面

### 主页面功能
- **基础搜索**: 输入CIK查询机构持仓
- **高级分析**: 访问 `/analysis.html` 进行详细分析
- **数据爬取**: 访问 `/scraping.html` 管理数据爬取

### 分析页面功能
- **综合概览**: 机构信息、投资组合摘要、重仓持股
- **重仓持股**: 详细的持股列表和排序
- **投资组合摘要**: 统计信息和行业分布
- **持仓变化**: 期间对比和变化分析

### 数据爬取页面功能
- **单个机构爬取**: 指定CIK爬取特定机构数据
- **批量爬取**: 一键爬取多个知名机构数据
- **任务监控**: 实时监控爬取任务进度和状态
- **任务管理**: 清理完成任务，查看详细日志

## 扩展功能

### 生产环境部署
- ✅ 真实的SEC数据爬取功能
- 实现更多报告格式 (PDF, Excel)
- 添加缓存机制提高性能
- 实现用户认证和权限管理
- 数据库集群和负载均衡

### 法律合规
- ✅ 遵守SEC网站服务条款
- ✅ 实施请求频率限制 (100ms间隔)
- ✅ 确保数据准确性验证
- ✅ 自动数据验证和清理

## 技术细节

### 性能优化
- 使用连接池优化数据库访问
- 实现查询结果缓存
- 异步处理大数据量查询

### 错误处理
- 完整的异常捕获和处理
- 用户友好的错误消息
- 详细的日志记录

## 真实数据源

### SEC EDGAR官方API
系统直接从SEC EDGAR数据库获取真实数据：
- **数据源**: `data.sec.gov` 官方API
- **免费使用**: 无需API密钥或付费
- **实时更新**: 跟随SEC发布节奏更新

### 示例机构数据
系统初始包含阿里巴巴控股集团示例数据，并支持爬取：
- 伯克希尔哈撒韦 (Berkshire Hathaway)
- 贝莱德 (BlackRock)
- 先锋集团 (Vanguard)
- 道富公司 (State Street)
- 富达投资 (Fidelity)
- 等数百家投资机构的真实持仓数据

### 数据覆盖范围
- **历史数据**: 2013年至今的所有13F文件
- **更新频率**: 季度更新 (与SEC发布同步)
- **数据完整性**: 包含完整的持仓明细和元数据

## 法律声明

此工具仅供教育和研究目的使用。用户应遵守SEC网站的使用条款和服务协议。所有数据均来自公开的SEC EDGAR数据库。

---

**注意**: 本项目仅用于学习和研究目的。在生产环境中使用时，请确保遵守相关法律法规和SEC的服务条款。