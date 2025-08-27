# SEC Data Collector - Spring Boot Application

## 项目架构

这是一个重构后的SEC 13F数据收集器，采用Spring Boot + Spring MVC架构，按照以下模块组织：

```
sec-data-collector/                    # 父模块
├── sec-data-collector-repository      # 数据访问层 (Repository)
├── sec-data-collector-service         # 业务逻辑层 (Service)
└── sec-data-collector-web             # Web接口层 (Controller)
```

## 技术栈

**后端框架：**
- Spring Boot 2.7.12
- Spring MVC (Web)
- Spring Context (依赖注入)
- Spring Boot Actuator (监控)

**数据层：**
- SQLite 数据库
- MyBatis ORM
- 自定义DAO层

**构建工具：**
- Maven 多模块项目
- Spring Boot Maven Plugin

## 快速开始

### 1. 构建项目

```bash
# 编译所有模块
mvn clean compile

# 打包应用
mvn package -DskipTests
```

### 2. 运行应用

```bash
# 方式1：使用Maven插件运行
cd sec-data-collector-web
mvn spring-boot:run

# 方式2：直接运行JAR包
java -jar sec-data-collector-web/target/sec-data-collector-web-1.0.0.jar
```

### 3. 访问应用

- **主页**: http://localhost:8080
- **API接口**: http://localhost:8080/api/*
- **健康检查**: http://localhost:8080/actuator/health

## API接口

### 数据抓取接口
```
POST /api/scraping/scrape              # 启动公司数据抓取
POST /api/scraping/scrape-latest       # 抓取最新13F文件
POST /api/scraping/scrape-batch        # 批量抓取
GET  /api/scraping/status              # 获取任务状态
GET  /api/scraping/tasks               # 获取所有任务
```

### 数据分析接口
```
GET /api/analysis/overview             # 机构概览
GET /api/analysis/top-holdings         # 热门持仓
GET /api/analysis/portfolio-summary    # 投资组合摘要
GET /api/analysis/holding-changes      # 持仓变化
```

### 搜索接口
```
GET /api/search/filings                # 搜索文件
GET /api/search/companies              # 搜索公司
```

### 任务管理接口
```
GET  /api/tasks/stats                  # 任务统计
GET  /api/tasks/running                # 运行中任务
GET  /api/tasks/failed                 # 失败任务
POST /api/tasks/retry-failed           # 重试失败任务
```

## 配置文件

主要配置位于 `sec-data-collector-web/src/main/resources/application.yml`:

```yaml
server:
  port: 8080                           # 服务端口

spring:
  application:
    name: sec-data-collector           # 应用名称

# 自定义配置
sec-collector:
  database:
    url: jdbc:sqlite:sec13f.db         # 数据库连接
    max-connections: 10
  scraping:
    thread-pool-size: 3                # 爬虫线程数
    request-delay-ms: 100              # 请求间隔
```

## 模块说明

### Repository层 (sec-data-collector-repository)
- **职责**: 数据访问、实体定义、数据库操作
- **主要类**: 
  - `FilingDAO` - 文件数据访问
  - `TaskDAO` - 任务数据访问
  - `ScrapingTask` - 任务实体
  - `Filing`, `Holding` - 业务实体

### Service层 (sec-data-collector-service)
- **职责**: 业务逻辑、数据处理、外部API调用
- **主要类**:
  - `DataScrapingService` - 数据抓取服务
  - `HoldingAnalysisService` - 持仓分析服务
  - `ScheduledScrapingService` - 定时任务服务

### Web层 (sec-data-collector-web)
- **职责**: HTTP接口、请求处理、响应格式化
- **主要类**:
  - `ScrapingController` - 抓取接口
  - `AnalysisController` - 分析接口
  - `TaskManagementController` - 任务管理接口

## 开发指南

### 添加新的API接口

1. 在相应的Controller中添加新方法
2. 使用`@GetMapping`, `@PostMapping`等注解定义路由
3. 注入需要的Service依赖
4. 返回`ResponseEntity<?>` 对象

示例：
```java
@RestController
@RequestMapping("/api/example")
public class ExampleController {
    
    @Autowired
    private ExampleService exampleService;
    
    @GetMapping("/data")
    public ResponseEntity<?> getData(@RequestParam String id) {
        try {
            Object data = exampleService.getData(id);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(createErrorResponse(e.getMessage()));
        }
    }
}
```

### 添加新的业务逻辑

1. 在Service层创建新的服务类
2. 在`ServiceConfig`中注册为Spring Bean
3. 在Controller中注入使用

### 数据库操作

1. 在Repository层的DAO类中添加新方法
2. 使用现有的数据库连接和事务管理
3. 遵循现有的异常处理模式

## 部署说明

### 生产环境部署

1. 构建生产包：`mvn clean package -Pprod`
2. 配置生产环境参数
3. 运行：`java -jar sec-data-collector-web-1.0.0.jar`

### Docker部署 (待实现)

```dockerfile
FROM openjdk:8-jre-slim
COPY sec-data-collector-web-1.0.0.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
```

## 迁移说明

此项目已从Jetty + Servlet架构完全迁移到Spring Boot + Spring MVC：

- ✅ 替换Jetty为Spring Boot内嵌Tomcat
- ✅ 将Servlet转换为Spring MVC Controller  
- ✅ 实现依赖注入和Bean管理
- ✅ 统一异常处理和响应格式
- ✅ 支持Spring Boot特性 (Actuator、DevTools等)

原有的功能完全保留，API接口保持兼容。