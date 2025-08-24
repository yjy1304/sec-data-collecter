# SEC 13F 真实数据爬取指南

本系统现在支持从SEC EDGAR数据库爬取真实的13F持仓数据。以下是详细的使用指南。

## 🚀 快速开始

### 1. 启动系统
```bash
mvn clean package -DskipTests
java -jar target/sec-13f-parser-1.0.0.jar
```

### 2. 访问爬取管理页面
打开浏览器访问: `http://localhost:8080/scraping.html`

## 📊 功能概述

### 数据源
- **官方SEC EDGAR API**: 直接从 `data.sec.gov` 获取数据
- **免费且无需API密钥**: 使用SEC官方公开API
- **合规性**: 自动遵守SEC的请求频率限制 (每10秒1个请求)

### 支持的机构类型
- 投资管理公司
- 对冲基金
- 养老基金
- 保险公司
- 银行信托部门
- 其他管理超过1亿美元资产的机构

## 🛠 使用方法

### 单个机构爬取

#### 方法1: Web界面
1. 访问 `http://localhost:8080/scraping.html`
2. 输入CIK号码和机构名称
3. 选择"爬取所有13F文件"或"仅爬取最新文件"
4. 监控任务进度

#### 方法2: API调用
```bash
# 爬取所有13F文件
curl -X POST "http://localhost:8080/api/scraping/scrape" \
  -d "cik=0001067983&companyName=Berkshire Hathaway Inc"

# 仅爬取最新文件
curl -X POST "http://localhost:8080/api/scraping/scrape-latest" \
  -d "cik=0001067983&companyName=Berkshire Hathaway Inc"
```

### 批量爬取
系统预配置了以下知名机构进行批量爬取：
- 阿里巴巴控股 (CIK: 0001524258)
- 伯克希尔哈撒韦 (CIK: 0001067983)
- 贝莱德 (CIK: 0001013594)
- 先锋集团 (CIK: 0000909832)
- 道富公司 (CIK: 0001364742)

```bash
# 批量爬取
curl -X POST "http://localhost:8080/api/scraping/scrape-batch"
```

## 📈 监控和管理

### 任务状态查询
```bash
# 查看所有任务状态
curl "http://localhost:8080/api/scraping/tasks"

# 查看特定任务状态
curl "http://localhost:8080/api/scraping/status?taskId=scrape_0001067983_1692308734567"
```

### 任务管理
```bash
# 清理已完成任务
curl "http://localhost:8080/api/scraping/cleanup"
```

## 🎯 CIK号码查找

### 常见投资机构CIK
| 机构名称 | CIK | 类型 |
|---------|-----|------|
| Berkshire Hathaway Inc | 0001067983 | 投资公司 |
| BlackRock Inc | 0001013594 | 资产管理 |
| Vanguard Group Inc | 0000909832 | 资产管理 |
| State Street Corp | 0001364742 | 银行信托 |
| Fidelity Management & Research | 0000315066 | 资产管理 |
| JPMorgan Chase & Co | 0000019617 | 银行信托 |
| Goldman Sachs Group Inc | 0000886982 | 投资银行 |
| Morgan Stanley | 0000895421 | 投资银行 |

### CIK查找方法
1. **SEC官网查找**: https://www.sec.gov/edgar/searchedgar/cik.htm
2. **EDGAR数据库**: https://www.sec.gov/edgar/searchedgar/companysearch.html
3. **直接搜索**: 在EDGAR系统中搜索公司名称

## 🔧 技术详细

### 数据验证
系统自动进行数据验证，包括：
- CIK格式验证 (10位数字)
- CUSIP格式验证 (9位字母数字)
- 持仓数值合理性检查
- 日期有效性验证
- 公司名称规范化

### 错误处理
- 网络连接错误自动重试
- 数据格式错误记录并跳过
- 验证失败的文件不会保存
- 详细的错误日志和状态报告

### 性能优化
- 请求频率控制 (100ms间隔)
- 并发任务限制 (最多3个同时进行)
- 内存使用优化
- 自动任务清理

## 📋 数据格式

### 支持的文件类型
- 13F-HR (季度持仓报告)
- 13F-HR/A (修正版持仓报告)

### 数据结构
```json
{
  "cik": "0001067983",
  "companyName": "Berkshire Hathaway Inc",
  "filingType": "13F-HR",
  "filingDate": "2023-11-15",
  "accessionNumber": "0001067983-23-000042",
  "holdings": [
    {
      "nameOfIssuer": "Apple Inc",
      "cusip": "037833100",
      "value": 162824000000,
      "shares": 905560000
    }
  ]
}
```

## ⚠️ 重要注意事项

### 法律合规
1. **遵守SEC服务条款**: 不要过度请求
2. **商业用途**: 如用于商业目的，请确保合规
3. **数据准确性**: 独立验证重要投资决策的数据

### 技术限制
1. **请求频率**: 每10秒最多1个请求
2. **数据延迟**: 新文件可能需要1-2天处理
3. **文件格式**: 某些老文件可能无法解析

### 使用建议
1. **首次使用**: 先爬取单个最新文件测试
2. **批量操作**: 在非工作时间进行大批量爬取
3. **数据备份**: 定期备份SQLite数据库文件

## 🔍 故障排除

### 常见问题

#### 问题1: 爬取任务失败
**可能原因**: 网络连接问题、CIK错误、SEC服务器维护
**解决方案**: 
- 检查网络连接
- 验证CIK格式
- 稍后重试

#### 问题2: 数据验证失败
**可能原因**: 数据格式不标准、XML解析错误
**解决方案**:
- 查看详细错误信息
- 手动验证SEC网站上的文件
- 联系技术支持

#### 问题3: 任务进度停滞
**可能原因**: SEC服务器响应慢、请求频率限制
**解决方案**:
- 耐心等待
- 检查任务状态
- 必要时重启服务

### 日志分析
系统会记录详细日志，包括：
- 请求时间戳
- 响应状态码
- 数据验证结果
- 错误详细信息

## 📞 技术支持

如遇到技术问题，请提供以下信息：
1. 使用的CIK号码
2. 错误信息截图
3. 任务ID
4. 预期行为描述

---

**免责声明**: 本工具仅供教育和研究用途。用户需自行承担使用风险，并确保遵守相关法律法规。