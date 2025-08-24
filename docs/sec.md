# SEC的免费数据接口
SEC确实提供了免费的API来获取13F数据，主要通过EDGAR API系统。这个接口是完全免费的，无需付费即可使用。
主要接口选项
1. EDGAR API (推荐)

网址: https://www.sec.gov/edgar/sec-api-documentation
特点: RESTful API，返回JSON格式数据
速率限制: 每秒10次请求
数据范围: 包括13F-HR/A表格在内的所有EDGAR文件

2. EDGAR批量数据

网址: https://www.sec.gov/dera/data
特点: 提供历史13F数据的批量下载
格式: CSV和XML格式

申请使用权限步骤
基本要求

设置User-Agent头部

格式: Sample Company Name AdminContact@<sample company domain>.com
这是必需的，否则请求会被拒绝


遵守速率限制

每秒不超过10次请求
建议在请求之间添加延迟



使用示例
pythonimport requests
import time

headers = {
'User-Agent': 'Your Company Name yourname@example.com'
}

# 获取特定公司的13F提交
url = 'https://data.sec.gov/submissions/CIK##########.json'
response = requests.get(url, headers=headers)

# 请求之间添加延迟
time.sleep(0.1)
注意事项

无需注册: 不需要申请API密钥或注册账户
数据延迟: 13F报告通常在季度结束后45天内提交
数据格式: 可获取XML、JSON、HTML等多种格式
历史数据: 可以获取2013年以后的所有13F数据

其他选择
如果需要更结构化的数据或更高的速率限制，可以考虑：

SEC的DERA数据库（批量下载）
第三方数据提供商（如Refinitiv、Bloomberg等，但这些通常收费）

您是否需要我提供具体的代码示例来开始使用这些接口？