# HTTP请求超时问题修复

## 问题描述

在任务执行过程中，`RealSECScraper.get13FDetails()` 方法中的HTTP请求会阻塞，导致整个任务系统无法正常工作。具体表现为：

- 任务状态一直保持在执行中，无法完成
- 系统资源被占用，影响其他任务的执行
- 没有合适的超时机制来处理网络异常

## 根本原因

`RealSECScraper` 类使用 Apache HttpClient 进行网络请求，但没有配置超时参数：

```java
// 原来的代码 - 没有超时配置
this.httpClient = HttpClientBuilder.create()
    .setUserAgent("SEC13F Analysis Tool admin@sec13fparser.com")
    .build();
```

这导致当SEC网站响应慢或网络不稳定时，请求会无限期地等待。

## 修复方案

### 1. 添加HTTP客户端超时配置

在 `RealSECScraper` 构造函数中配置3秒超时：

```java
public RealSECScraper() {
    // 配置3秒超时的请求配置
    RequestConfig requestConfig = RequestConfig.custom()
        .setConnectionRequestTimeout(3000)  // 从连接池获取连接的超时时间
        .setConnectTimeout(3000)           // 建立连接的超时时间  
        .setSocketTimeout(3000)            // 数据传输的超时时间
        .build();
        
    this.httpClient = HttpClientBuilder.create()
        .setUserAgent("SEC13F Analysis Tool admin@sec13fparser.com")
        .setDefaultRequestConfig(requestConfig)
        .build();
    // ...
}
```

### 2. 增强HTTP请求的异常处理和日志

在 `executeGetRequest()` 方法中添加：

- 详细的请求执行时间日志
- 针对不同类型异常的具体处理
- 超时异常的专门捕获和记录

```java
private String executeGetRequest(String url) throws IOException {
    // ... 请求设置 ...
    
    long startTime = System.currentTimeMillis();
    try {
        logger.debug("🌐 执行HTTP请求: " + url + " (3秒超时)");
        HttpResponse response = httpClient.execute(request);
        long duration = System.currentTimeMillis() - startTime;
        // ... 成功处理 ...
        
    } catch (java.net.SocketTimeoutException e) {
        long duration = System.currentTimeMillis() - startTime;
        logger.warn("⏰ HTTP请求超时: " + url + " (耗时: " + duration + "ms)");
        throw new IOException("Request timeout after 3 seconds for URL: " + url, e);
    } catch (java.net.ConnectException e) {
        long duration = System.currentTimeMillis() - startTime;
        logger.warn("🔌 连接失败: " + url + " (耗时: " + duration + "ms)");
        throw new IOException("Connection failed for URL: " + url, e);
    }
    // ... 其他异常处理 ...
}
```

### 3. 任务级别的超时处理

在 `ScrapingTaskProcessPlugin` 中增强异常处理：

```java
} catch (java.io.IOException e) {
    if (e.getMessage() != null && e.getMessage().contains("timeout")) {
        String errorMessage = "网络请求超时: " + e.getMessage();
        logger.warn("⏰ 抓取任务网络超时，将重试: " + errorMessage);
        return TaskResult.failure(errorMessage, e);
    }
    // ... 其他IOException处理 ...
} catch (InterruptedException e) {
    String errorMessage = "抓取任务被中断: " + e.getMessage();
    logger.warn("🛑 抓取任务被中断: " + errorMessage);
    Thread.currentThread().interrupt();
    return TaskResult.failure(errorMessage, e);
}
```

## 修复效果

### 1. 请求超时控制 ✅
- 所有HTTP请求现在都有3秒超时限制
- 连接建立、数据传输都有独立的超时设置
- 防止任务因网络问题而无限期阻塞

### 2. 详细的日志记录 ✅
- 记录每个HTTP请求的执行时间
- 区分不同类型的网络异常
- 便于调试和监控网络问题

### 3. 任务重试机制 ✅
- 超时任务会被标记为失败，触发重试机制
- 网络异常和超时被区别对待
- 保持任务系统的稳定性

### 4. 资源管理 ✅
- 避免了因网络阻塞而占用系统资源
- 任务能够快速失败并释放线程池资源

## 测试建议

### 1. 正常情况测试
```bash
# 创建正常的抓取任务
curl -X POST "http://localhost:8080/api/scraping/scrape?cik=0001524258&companyName=Alibaba"

# 观察日志中的HTTP请求时间记录
# 期望看到：🌐 执行HTTP请求... ⏱️ 请求完成，耗时: XXXms
```

### 2. 超时情况模拟
可以通过以下方式模拟测试：

1. **修改超时时间**：临时将超时时间改为100ms来快速触发超时
2. **网络环境测试**：在网络较差的环境下执行任务
3. **负载测试**：同时创建多个任务，观察超时处理

### 3. 日志监控
关注以下日志模式：

- **正常请求**：`⏱️ 请求完成，耗时: XXXms, 状态码: 200`
- **超时异常**：`⏰ HTTP请求超时: URL (耗时: ~3000ms)`
- **连接异常**：`🔌 连接失败: URL (耗时: XXXms)`
- **任务重试**：`⏰ 抓取任务网络超时，将重试`

## 配置调整

如果需要调整超时时间，可以修改 `RequestConfig` 中的参数：

```java
RequestConfig requestConfig = RequestConfig.custom()
    .setConnectionRequestTimeout(5000)  // 调整为5秒
    .setConnectTimeout(5000)           // 调整为5秒
    .setSocketTimeout(5000)            // 调整为5秒
    .build();
```

## 后续优化建议

1. **配置外部化**：将超时时间配置到 `application.yml` 中
2. **指数退避重试**：为网络失败的任务实现更智能的重试策略
3. **连接池优化**：配置合适的HTTP连接池参数
4. **监控指标**：添加网络请求成功率、平均响应时间等监控指标

---

**总结**：这次修复解决了任务系统中最关键的阻塞问题，确保系统能够优雅地处理网络异常，并通过重试机制保证任务的最终完成。3秒的超时设置在保证数据获取成功率的同时，也避免了长时间的资源占用。