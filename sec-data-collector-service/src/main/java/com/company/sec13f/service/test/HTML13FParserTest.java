package com.company.sec13f.service.test;

import com.company.sec13f.repository.model.Filing;
import com.company.sec13f.service.parser.HTML13FParser;
import com.company.sec13f.service.util.Logger;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

/**
 * HTML13FParser测试类 - 验证HTML解析功能
 */
public class HTML13FParserTest {
    
    private static final Logger logger = Logger.getInstance();
    
    /**
     * 测试HTML解析器能否正确解析SEC 13F HTML文件
     */
    public static void testRealSECFile() {
        logger.info("🧪 开始测试HTML13FParser...");
        
        // 使用伯克希尔哈撒韦的一个真实13F文件
        String testUrl = "https://www.sec.gov/Archives/edgar/data/1067983/000095012325005701/xslForm13F_X02/form13fInfoTable.xml";
        
        try {
            // 获取HTML内容
            logger.info("📥 获取测试文件: " + testUrl);
            String htmlContent = fetchContent(testUrl);
            
            if (htmlContent == null) {
                logger.error("❌ 无法获取测试文件内容");
                return;
            }
            
            logger.info("📄 文件大小: " + htmlContent.length() + " 字符");
            
            // 使用HTML解析器解析
            logger.info("🔍 开始HTML解析测试...");
            Filing filing = HTML13FParser.parseHTML13FContent(htmlContent, "test-accession-001", "0001067983");
            
            // 输出结果
            if (filing != null) {
                logger.info("✅ 解析成功!");
                logger.info("📋 申报编号: " + filing.getAccessionNumber());
                logger.info("🏢 CIK: " + filing.getCik());
                logger.info("📅 申报日期: " + filing.getFilingDate());
                logger.info("📅 报告期间: " + filing.getReportPeriod());
                logger.info("📊 持仓数量: " + (filing.getHoldings() != null ? filing.getHoldings().size() : 0));
                
                if (filing.getHoldings() != null && !filing.getHoldings().isEmpty()) {
                    logger.info("\n=== 持仓详情 (前5条) ===");
                    for (int i = 0; i < Math.min(5, filing.getHoldings().size()); i++) {
                        com.company.sec13f.repository.model.Holding holding = filing.getHoldings().get(i);
                        logger.info((i+1) + ". " + holding.getNameOfIssuer() + 
                                   " (CUSIP: " + holding.getCusip() + 
                                   ", 价值: " + holding.getValue() + 
                                   ", 股数: " + holding.getShares() + ")");
                    }
                    
                    if (filing.getHoldings().size() > 5) {
                        logger.info("... 还有 " + (filing.getHoldings().size() - 5) + " 条记录");
                    }
                } else {
                    logger.error("❌ 没有解析到任何持仓记录!");
                }
                
            } else {
                logger.error("❌ 解析失败，返回null");
            }
            
        } catch (Exception e) {
            logger.error("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        logger.info("🧪 HTML解析器测试完成");
    }
    
    /**
     * 获取URL内容
     */
    private static String fetchContent(String url) throws Exception {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpGet request = new HttpGet(url);
            request.setHeader("User-Agent", "SEC 13F Parser Test academic@research.edu");
            
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
     * 主方法
     */
    public static void main(String[] args) {
        testRealSECFile();
    }
}