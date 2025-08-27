# SEC官方API使用指南

## 🏛️ SEC EDGAR数据源概览

美国证券交易委员会（SEC）通过EDGAR系统提供完全免费的公开数据访问，包括13F机构持仓报告。本系统直接使用这些官方API获取真实、及时的金融数据。

## 🌐 主要数据接口

### 1. EDGAR REST API（推荐）

**官方文档**: https://www.sec.gov/edgar/sec-api-documentation

**接口特点**:
- 完全免费，无需注册或API密钥
- RESTful设计，返回JSON格式数据
- 支持程序化访问所有EDGAR文件
- 实时更新，与官方发布同步

**数据覆盖范围**:
- 13F-HR: 机构持仓报告
- 13F-HR/A: 修正后的持仓报告  
- 10-K, 10-Q: 年报和季报
- 8-K: 重大事件报告
- 其他所有SEC要求的文件类型

### 2. EDGAR批量数据下载

**官方链接**: https://www.sec.gov/dera/data

**适用场景**:
- 历史数据批量分析
- 学术研究大规模数据处理
- 离线数据分析需求

**数据格式**:
- CSV格式：结构化数据表格
- XML格式：完整的原始提交文件

## 🔧 API使用规范

### 必需的请求头设置

**User-Agent要求**:
```
User-Agent: [Your Company/Organization Name] [Contact Email]
```

**示例**:
```
User-Agent: Academic Research Project researcher@university.edu
User-Agent: Investment Analysis Tool support@company.com
```

**重要性**:
- SEC要求所有请求必须包含标识信息
- 缺少或不规范的User-Agent会导致请求被拒绝（HTTP 403）
- 有助于SEC监控API使用情况和提供技术支持

### 速率限制规定

**官方限制**:
- **每秒最多10次请求**
- 建议请求间隔：100-150毫秒
- 持续高频访问可能被暂时限制

**本系统的实现**:
```java
// RealSECScraper.java中的实现
private static final int REQUEST_DELAY_MS = 100;  // 100ms间隔

private void respectRateLimit() {
    try {
        Thread.sleep(REQUEST_DELAY_MS);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}
```

## 📊 数据获取流程

### 第一步：获取机构基本信息

**API端点**:
```
https://data.sec.gov/submissions/CIK{10位CIK}.json
```

**示例请求**:
```bash
curl -H "User-Agent: SEC13F Parser research@example.com" \
  "https://data.sec.gov/submissions/CIK0001067983.json"
```

**响应数据结构**:
```json
{
  "cik": "0001067983",
  "entityType": "operating",
  "sic": "6331",
  "sicDescription": "Fire, Marine & Casualty Insurance",
  "name": "BERKSHIRE HATHAWAY INC",
  "filings": {
    "recent": {
      "accessionNumber": ["0000950123-25-008361", ...],
      "filingDate": ["2025-02-14", ...],
      "reportDate": ["2024-12-31", ...],
      "acceptanceDateTime": ["2025-02-14T18:30:09.000Z", ...],
      "form": ["13F-HR", "13F-HR", ...]
    }
  }
}
```

### 第二步：筛选13F报告

**目标文件类型**:
- `13F-HR`: 季度机构持仓报告
- `13F-HR/A`: 修正版本

**筛选逻辑**:
```java
// 在filings.recent.form数组中查找13F相关文件
List<String> forms = responseData.getFilings().getRecent().getForm();
for (int i = 0; i < forms.size(); i++) {
    if (forms.get(i).startsWith("13F")) {
        String accessionNumber = accessionNumbers.get(i);
        String filingDate = filingDates.get(i);
        // 处理13F文件
    }
}
```

### 第三步：获取具体13F文件内容

**文件URL构造**:
```
https://www.sec.gov/Archives/edgar/data/{CIK}/{AccessionNumber}/{FileName}
```

**常见文件名模式**:
- `{AccessionNumber}.txt`: 主要提交文件
- `{AccessionNumber}-index.htm`: 文件索引
- `infotable.xml`: 13F持仓明细表（XML格式）
- `form13fInfoTable.xml`: 持仓信息表的另一种命名

**获取持仓数据示例**:
```bash
curl -H "User-Agent: SEC13F Parser research@example.com" \
  "https://www.sec.gov/Archives/edgar/data/1067983/000095012325008361/infotable.xml"
```

## 📋 13F数据结构解析

### XML文档结构

```xml
<?xml version="1.0" encoding="UTF-8"?>
<informationTable>
    <infoTable>
        <nameOfIssuer>APPLE INC</nameOfIssuer>
        <titleOfClass>COM</titleOfClass>
        <cusip>037833100</cusip>
        <value>47041969</value>
        <sshPrnamt>400000000</sshPrnamt>
        <sshPrnamtType>SH</sshPrnamtType>
        <investmentDiscretion>SOLE</investmentDiscretion>
        <otherManager>0</otherManager>
    </infoTable>
    <!-- 更多持仓记录 -->
</informationTable>
```

### 关键字段说明

| XML字段 | 中文含义 | 数据库字段 | 说明 |
|---------|----------|------------|------|
| `nameOfIssuer` | 发行人名称 | `name_of_issuer` | 股票发行公司名称 |
| `cusip` | CUSIP标识符 | `cusip` | 9位证券标识码 |
| `value` | 持仓市值 | `value` | 单位：千美元 |
| `sshPrnamt` | 股份数量 | `shares` | 持有股票数量 |
| `sshPrnamtType` | 数量类型 | - | 通常为"SH"(股份) |

## 🔄 数据处理工作流

### 系统实现的完整流程

```java
// 1. 获取机构信息
CompanySubmissions submissions = scraper.getCompanySubmissions(cik);

// 2. 筛选13F文件
List<Filing> filings = filterAndExtract13FFilings(submissions);

// 3. 解析每个13F文件的持仓数据
for (Filing filing : filings) {
    List<String> infoTableUrls = scraper.findInfoTableUrls(filing);
    for (String url : infoTableUrls) {
        String xmlContent = scraper.downloadXmlContent(url);
        List<Holding> holdings = parser.parseInformationTable(xmlContent);
        filing.setHoldings(holdings);
    }
}

// 4. 保存到数据库
filingRepositoryService.saveFiling(filing);
```

### 错误处理和重试机制

**常见错误类型**:
- HTTP 403: User-Agent不规范或请求过于频繁
- HTTP 404: 文件不存在或URL错误
- HTTP 503: SEC服务器临时不可用
- 网络超时: 连接或读取超时

**重试策略**:
```java
@Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2.0))
public String downloadWithRetry(String url) {
    // HTTP请求实现
}
```

## 📅 数据更新时间表

### 13F报告提交周期

**法规要求**:
- 季度报告：每季度结束后45天内提交
- 修正报告：发现错误后及时提交修正版本

**典型时间表**:
- Q1 (1-3月): 5月15日前提交
- Q2 (4-6月): 8月14日前提交  
- Q3 (7-9月): 11月14日前提交
- Q4 (10-12月): 次年2月14日前提交

### 数据可用性

**实时性**:
- SEC接受文件后立即在EDGAR系统可见
- API通常在文件发布后几分钟内可访问
- 本系统支持实时数据获取

## 🔐 合规性和最佳实践

### 使用条款遵守

**SEC要求**:
1. 设置合规的User-Agent标识
2. 遵守速率限制（每秒不超过10次请求）
3. 不得进行恶意或滥用性访问
4. 用于合法的商业、学术或个人研究目的

### 技术最佳实践

1. **请求优化**:
   ```java
   // 使用连接池复用连接
   HttpClient httpClient = HttpClientBuilder.create()
       .setMaxConnTotal(10)
       .setMaxConnPerRoute(5)
       .build();
   ```

2. **错误日志记录**:
   ```java
   logger.info("正在获取CIK {} 的数据，URL: {}", cik, url);
   logger.warn("请求失败，状态码: {}，将在{}ms后重试", statusCode, retryDelay);
   ```

3. **数据验证**:
   ```java
   // 验证CIK格式
   if (!cik.matches("\\d{10}")) {
       throw new IllegalArgumentException("Invalid CIK format: " + cik);
   }
   
   // 验证CUSIP格式
   if (!cusip.matches("[0-9A-Z]{9}")) {
       throw new IllegalArgumentException("Invalid CUSIP format: " + cusip);
   }
   ```

## 🚀 扩展可能性

### 其他可用数据类型

**基本信息**:
- 10-K年报：完整的财务状况和业务概述
- 10-Q季报：季度财务更新
- 8-K临时报告：重大事件披露

**机构专用**:
- Form ADV：投资顾问注册信息
- Schedule 13D/G：股东权益披露
- Form 4：内部人员交易报告

### 数据丰富化

**可结合的外部数据**:
- 股票价格数据：计算实时持仓价值
- 行业分类数据：GICS行业分析
- 财务指标数据：基本面分析
- 市场数据：相对表现分析

## ❗ 重要提醒

### 数据使用声明

**合法用途**:
- ✅ 学术研究和教育目的
- ✅ 投资决策参考（非建议）
- ✅ 金融分析和报告
- ✅ 合规监管用途

**注意事项**:
- 📊 数据基于机构报告，存在报告时间滞后
- 🔍 13F仅披露股票持仓，不包括债券、衍生品等
- ⚠️ 数据仅供参考，不构成投资建议
- 📝 使用时应注明数据来源为SEC EDGAR

### 技术限制

**系统限制**:
- SQLite数据库：适合中小规模数据处理
- 单机部署：不适合高并发访问
- 实时分析：受SEC API速率限制

**建议改进**:
- 生产环境可考虑使用PostgreSQL/MySQL
- 实施缓存策略减少API调用
- 构建数据仓库支持复杂分析需求