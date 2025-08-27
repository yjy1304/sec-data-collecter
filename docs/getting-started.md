# 快速开始指南

## 🚀 环境准备

### 系统要求
- **Java**: JDK 8 或更高版本
- **Maven**: 3.6 或更高版本  
- **内存**: 建议至少 512MB 可用内存
- **磁盘空间**: 至少 1GB 用于数据存储

### 验证环境
```bash
# 检查Java版本
java -version

# 检查Maven版本
mvn -version
```

## 📦 项目获取和构建

### 1. 获取源代码
```bash
# 克隆项目（如果使用Git）
git clone <repository-url>
cd sec-13f-parser
```

### 2. 构建项目
```bash
# 首先构建持久化模块
cd sec-data-collector-repository
mvn clean install

# 返回主目录构建主应用
cd ..
mvn clean compile
mvn package -DskipTests
```

### 3. 验证构建
```bash
# 检查生成的JAR文件
ls -la target/sec-13f-parser-1.0.0.jar
```

## 🏃‍♂️ 启动应用

### 方式一：直接运行JAR包
```bash
java -jar target/sec-13f-parser-1.0.0.jar
```

### 方式二：使用Maven插件
```bash
mvn exec:java -Dexec.mainClass="com.company.sec13f.parser.WebServer"
```

### 启动成功标识
看到以下输出表示启动成功：
```
🚀 Starting Repository Module Test...
🏗️ Initializing database tables...
Database tables initialized successfully
Server started on port 8080
Visit http://localhost:8080 to access the application
```

## 🌐 访问应用

### Web界面入口
- **主页**: http://localhost:8080
- **高级分析**: http://localhost:8080/analysis.html  
- **数据爬取**: http://localhost:8080/scraping.html
- **数据库管理**: http://localhost:8080/database.html

### 基本功能测试

#### 1. 测试基础搜索
```bash
# 搜索阿里巴巴的13F数据
curl "http://localhost:8080/search?cik=0001524258"
```

#### 2. 测试分析API
```bash
# 获取机构概览
curl "http://localhost:8080/api/analysis/overview?cik=0001524258"

# 获取重仓持股
curl "http://localhost:8080/api/analysis/top-holdings?cik=0001524258&limit=10"
```

## 💾 测试持久化模块

### 运行Repository模块测试
```bash
cd sec-data-collector-repository
mvn exec:java -Dexec.mainClass="com.company.sec13f.repository.test.RepositoryTest"
```

### 预期输出
```
🚀 Starting Repository Module Test...
🏗️ Initializing database tables...
📄 Testing Filing operations...
✅ Filing saved with ID: 1
✅ Filing retrieved: Berkshire Hathaway Inc with 2 holdings
📝 Testing Task operations...
✅ Task saved with ID: 1
✅ All tests completed successfully!
```

## 📊 数据爬取快速体验

### 1. 单个机构数据爬取
```bash
# 爬取伯克希尔哈撒韦数据
curl -X POST "http://localhost:8080/api/scraping/scrape" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "cik=0001067983&companyName=Berkshire+Hathaway+Inc"
```

### 2. 查看任务状态
```bash
# 获取所有任务
curl "http://localhost:8080/api/scraping/tasks"

# 查看特定任务状态
curl "http://localhost:8080/api/scraping/status/[taskId]"
```

### 3. 批量数据爬取
```bash
# 批量爬取知名机构数据
curl -X POST "http://localhost:8080/api/scraping/scrape-batch"
```

## 🔧 配置说明

### 数据库配置
数据库文件位置：`sec13f.db`（项目根目录）

### 端口配置
默认端口：8080  
如需修改，请编辑 `WebServer.java` 中的 `PORT` 常量。

### SEC API配置
系统会自动遵循以下规则：
- **User-Agent**: 自动设置符合SEC要求的标识
- **请求间隔**: 100ms最小间隔
- **重试机制**: 自动重试失败请求

## 🎯 常用CIK示例

以下是一些知名机构的CIK，可用于测试：

| 机构名称 | CIK | 说明 |
|---------|-----|------|
| Berkshire Hathaway | 0001067983 | 巴菲特的伯克希尔哈撒韦 |
| Alibaba Group | 0001524258 | 阿里巴巴集团（示例数据）|
| BlackRock | 0001364742 | 全球最大资产管理公司 |
| Vanguard | 0001070048 | 指数基金先驱 |
| State Street | 0000093751 | ETF巨头 |

## 🛠️ 开发模式

### 前端开发
前端文件位于：`src/main/resources/webapp/`
- 修改HTML/CSS/JS文件后，重启服务器生效
- 支持热重载（需要额外配置）

### 后端开发
- 修改Java代码后需要重新编译：`mvn compile`
- 使用IDE的Debug模式可以进行断点调试

### 数据库操作
```bash
# 查看数据库信息
./db_info.sh

# 直接访问SQLite数据库
sqlite3 sec13f.db
.tables
.schema filings
```

## 📝 日志查看

### 应用日志
```bash
# 查看运行日志
tail -f sec13f-parser.log

# 查看所有日志
cat sec13f-parser.log
```

### 任务日志
- Web界面：http://localhost:8080/scraping.html
- 实时查看任务执行状态和进度

## 🚨 常见问题

### 端口被占用
```bash
# 查看端口使用情况
lsof -i :8080

# 杀掉占用端口的进程
kill -9 <PID>
```

### 数据库权限问题
```bash
# 确保数据库文件有写权限
chmod 666 sec13f.db
chmod 755 .
```

### Maven构建失败
```bash
# 清理并重新构建
mvn clean
cd sec-data-collector-repository
mvn clean install
cd ..
mvn clean package -DskipTests
```

### SEC API访问失败
- 检查网络连接
- 确认User-Agent设置正确
- 验证请求频率不超过限制

## 🎉 下一步

恭喜！你已经成功启动了SEC 13F Parser。现在你可以：

1. **探索Web界面**: 访问各个页面了解功能
2. **查看文档**: 阅读 [API文档](api-reference.md) 了解更多接口
3. **数据分析**: 尝试分析不同机构的持仓数据
4. **自定义开发**: 基于现有架构扩展新功能

需要更多帮助？查看：
- [架构设计文档](architecture.md)
- [数据库设计文档](database-schema.md)
- [故障排除指南](troubleshooting.md)