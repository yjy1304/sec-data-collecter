package com.company.sec13f.service.util;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTML内容分析工具 - 用于调试SEC HTML格式解析问题
 */
public class HTMLContentAnalyzer {
    
    private static final Logger logger = Logger.getInstance();
    
    /**
     * 分析SEC HTML内容并输出调试信息
     */
    public static void analyzeContent(String url) {
        try {
            // 获取内容
            String content = fetchContent(url);
            if (content == null) {
                logger.error("❌ 无法获取内容: " + url);
                return;
            }
            
            logger.info("🔍 分析URL: " + url);
            logger.info("📄 内容长度: " + content.length() + " 字符");
            
            // 检测格式
            analyzeFormat(content);
            
            // 查找表格结构
            analyzeTableStructure(content);
            
            // 查找持仓数据
            analyzeHoldingsData(content);
            
        } catch (Exception e) {
            logger.error("❌ 内容分析失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取URL内容
     */
    private static String fetchContent(String url) throws IOException {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpGet request = new HttpGet(url);
            request.setHeader("User-Agent", "SEC 13F Parser academic@research.edu");
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    logger.error("❌ HTTP错误: " + statusCode);
                    return null;
                }
                
                HttpEntity entity = response.getEntity();
                return EntityUtils.toString(entity, "UTF-8");
            }
        }
    }
    
    /**
     * 分析内容格式
     */
    private static void analyzeFormat(String content) {
        logger.info("\n=== 格式分析 ===");
        
        String firstLine = content.split("\n")[0].trim();
        logger.info("📝 第一行: " + firstLine);
        
        // 检测各种标识符
        boolean hasHtmlTag = content.toLowerCase().contains("<html");
        boolean hasBodyTag = content.toLowerCase().contains("<body");
        boolean hasTableTag = content.toLowerCase().contains("<table");
        boolean hasMetaTag = content.toLowerCase().contains("<meta");
        boolean hasXmlDeclaration = content.trim().startsWith("<?xml");
        boolean hasFormDataClass = content.toLowerCase().contains("class=\"formdata");
        
        logger.info("🏷️ HTML标签: " + hasHtmlTag);
        logger.info("🏷️ BODY标签: " + hasBodyTag);
        logger.info("🏷️ TABLE标签: " + hasTableTag);
        logger.info("🏷️ META标签: " + hasMetaTag);
        logger.info("🏷️ XML声明: " + hasXmlDeclaration);
        logger.info("🏷️ FormData样式: " + hasFormDataClass);
        
        // 判断格式
        if (hasHtmlTag || hasBodyTag || (hasTableTag && hasFormDataClass)) {
            logger.info("✅ 检测为HTML格式");
        } else if (hasXmlDeclaration) {
            logger.info("✅ 检测为XML格式");
        } else {
            logger.info("⚠️ 格式不明确");
        }
    }
    
    /**
     * 分析表格结构
     */
    private static void analyzeTableStructure(String content) {
        logger.info("\n=== 表格结构分析 ===");
        
        // 查找表格
        Pattern tablePattern = Pattern.compile("<table[^>]*>(.*?)</table>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher tableMatcher = tablePattern.matcher(content);
        
        int tableCount = 0;
        while (tableMatcher.find()) {
            tableCount++;
            logger.info("📊 找到表格 " + tableCount);
            
            // 分析表格行
            String tableContent = tableMatcher.group(1);
            Pattern rowPattern = Pattern.compile("<tr[^>]*>(.*?)</tr>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher rowMatcher = rowPattern.matcher(tableContent);
            
            int rowCount = 0;
            while (rowMatcher.find() && rowCount < 3) { // 只分析前3行
                rowCount++;
                String rowContent = rowMatcher.group(1);
                logger.info("  第" + rowCount + "行内容: " + cleanText(rowContent).substring(0, Math.min(100, cleanText(rowContent).length())) + "...");
                
                // 分析列
                Pattern cellPattern = Pattern.compile("<td[^>]*>(.*?)</td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
                Matcher cellMatcher = cellPattern.matcher(rowContent);
                
                int cellCount = 0;
                while (cellMatcher.find()) {
                    cellCount++;
                    String cellContent = cleanText(cellMatcher.group(1));
                    if (!cellContent.trim().isEmpty() && cellCount <= 5) {
                        logger.info("    列" + cellCount + ": " + cellContent);
                    }
                }
            }
        }
        
        logger.info("📊 总共找到 " + tableCount + " 个表格");
    }
    
    /**
     * 分析持仓数据
     */
    private static void analyzeHoldingsData(String content) {
        logger.info("\n=== 持仓数据分析 ===");
        
        // 查找可能的持仓行
        Pattern[] patterns = {
            // 原始模式
            Pattern.compile("<tr>\\s*<td[^>]*class=\"FormData\"[^>]*>([^<]+)</td>\\s*<td[^>]*>([^<]*)</td>\\s*<td[^>]*class=\"FormData\"[^>]*>([^<]+)</td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            // 更简化的模式
            Pattern.compile("<td[^>]*class=\"FormData\"[^>]*>([A-Z][^<]+)</td>", Pattern.CASE_INSENSITIVE),
            // CUSIP模式
            Pattern.compile("<td[^>]*class=\"FormData\"[^>]*>([A-Z0-9]{9})</td>", Pattern.CASE_INSENSITIVE),
            // 数值模式
            Pattern.compile("<td[^>]*class=\"FormDataR\"[^>]*>([0-9,]+)</td>", Pattern.CASE_INSENSITIVE)
        };
        
        for (int i = 0; i < patterns.length; i++) {
            logger.info("🔍 模式 " + (i + 1) + " 匹配结果:");
            Matcher matcher = patterns[i].matcher(content);
            
            int matchCount = 0;
            while (matcher.find() && matchCount < 5) { // 只显示前5个匹配
                matchCount++;
                logger.info("  匹配" + matchCount + ": " + cleanText(matcher.group(1)));
            }
            
            if (matchCount == 0) {
                logger.info("  ❌ 没有匹配");
            }
        }
    }
    
    /**
     * 清理文本内容
     */
    private static String cleanText(String text) {
        if (text == null) return "";
        
        text = text.replaceAll("<[^>]+>", "");
        text = text.replaceAll("&nbsp;", " ");
        text = text.replaceAll("&amp;", "&");
        text = text.replaceAll("Â", "");
        
        return text.trim();
    }
    
    /**
     * 主方法用于测试
     */
    public static void main(String[] args) {
        // 测试伯克希尔哈撒韦的一个13F文件
        String testUrl = "https://www.sec.gov/Archives/edgar/data/1067983/000095012325005701/xslForm13F_X02/form13fInfoTable.xml";
        analyzeContent(testUrl);
    }
}