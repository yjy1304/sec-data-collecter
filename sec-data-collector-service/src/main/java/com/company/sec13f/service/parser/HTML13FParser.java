package com.company.sec13f.service.parser;

import com.company.sec13f.repository.model.Filing;
import com.company.sec13f.repository.model.Holding;
import com.company.sec13f.service.util.Logger;

import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTML格式的13F持仓信息解析器
 * 根据sec-api-guide.md中描述的HTML表格格式解析持仓数据
 */
public class HTML13FParser {
    
    private static final Logger logger = Logger.getInstance();
    
    /**
     * 解析HTML格式的13F持仓信息
     * @param htmlContent HTML内容
     * @param accessionNumber 申报编号
     * @param cik 机构CIK
     * @return Filing对象包含解析后的持仓信息
     */
    public static Filing parseHTML13FContent(String htmlContent, String accessionNumber, String cik) {
        Filing filing = new Filing();
        filing.setAccessionNumber(accessionNumber);
        filing.setCik(cik);
        filing.setFilingType("13F-HR");
        
        List<Holding> holdings = new ArrayList<>();
        
        try {
            // 解析HTML表格中的持仓数据
            holdings = parseHoldingsFromHTMLTable(htmlContent);
            logger.info("🎯 从HTML表格中解析出 " + holdings.size() + " 条持仓记录");
            
            // 尝试提取报告期间
            LocalDate reportPeriod = extractReportPeriod(htmlContent);
            if (reportPeriod != null) {
                filing.setReportPeriod(reportPeriod);
                filing.setFilingDate(reportPeriod); // 使用报告期间作为申报日期
            }
            
        } catch (Exception e) {
            logger.error("❌ 解析HTML格式13F文件失败: " + e.getMessage());
        }
        
        // 为每个holding设置CIK和公司名称（如果可用）
        for (Holding holding : holdings) {
            holding.setCik(cik);
            if (filing.getCompanyName() != null && !filing.getCompanyName().trim().isEmpty()) {
                holding.setCompanyName(filing.getCompanyName());
            }
        }
        
        filing.setHoldings(holdings);
        return filing;
    }
    
    /**
     * 从HTML表格中解析持仓信息
     * 根据实际的HTML结构分析结果优化解析逻辑
     */
    private static List<Holding> parseHoldingsFromHTMLTable(String htmlContent) {
        List<Holding> holdings = new ArrayList<>();
        
        try {
            logger.debug("🔍 开始解析HTML表格持仓数据...");
            
            // 基于实际HTML结构，持仓数据位于包含FormData样式的行中
            // 匹配持仓数据行的改进模式 - 更灵活地处理列结构
            Pattern holdingRowPattern = Pattern.compile(
                "<tr[^>]*>\\s*" +
                "<td[^>]*class=\"FormData\"[^>]*>([^<]+)</td>\\s*" +        // NAME OF ISSUER (发行人名称)
                "<td[^>]*class=\"FormData\"[^>]*>([^<]*)</td>\\s*" +         // TITLE OF CLASS (股票类别)
                "<td[^>]*class=\"FormData\"[^>]*>([A-Z0-9]{9})</td>\\s*" +   // CUSIP (9位代码)
                "(?:<td[^>]*>[^<]*</td>\\s*)*?" +                           // 可能的中间列（如FIGI）
                "<td[^>]*class=\"FormDataR\"[^>]*>([0-9,]+)</td>\\s*" +      // VALUE (价值，数字格式)
                "<td[^>]*class=\"FormDataR\"[^>]*>([0-9,]+)</td>\\s*" +      // SHARES (股数，数字格式)
                "<td[^>]*class=\"FormData\"[^>]*>([A-Z]+)</td>",             // SH/PRN TYPE (股票类型)
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
            );
            
            Matcher matcher = holdingRowPattern.matcher(htmlContent);
            int foundCount = 0;
            
            while (matcher.find()) {
                foundCount++;
                try {
                    String issuerName = cleanHtmlText(matcher.group(1));
                    String titleOfClass = cleanHtmlText(matcher.group(2));
                    String cusip = cleanHtmlText(matcher.group(3));
                    String value = cleanHtmlText(matcher.group(4));
                    String sharesAmount = cleanHtmlText(matcher.group(5));
                    String sharesPrinType = cleanHtmlText(matcher.group(6));
                    
                    logger.debug("📊 找到持仓数据 " + foundCount + ": " + issuerName + " | " + cusip + " | " + value + " | " + sharesAmount);
                    
                    // 验证必需字段
                    if (isValidData(issuerName) && isValidData(cusip) && isValidData(value)) {
                        Holding holding = new Holding();
                        
                        // 设置基本信息
                        holding.setNameOfIssuer(issuerName);
                        holding.setCusip(cusip);
                        
                        // 解析价值（SEC 13F中通常以千美元为单位）
                        try {
                            String cleanValue = value.replaceAll("[,$\\s]", "");
                            BigDecimal valueInThousands = new BigDecimal(cleanValue);
                            holding.setValue(valueInThousands);
                        } catch (NumberFormatException e) {
                            logger.debug("⚠️ 无法解析价值字段: " + value);
                            continue; // 跳过无法解析价值的记录
                        }
                        
                        // 解析股数（仅处理股票类型）
                        if (isValidData(sharesAmount) && "SH".equalsIgnoreCase(sharesPrinType)) {
                            try {
                                String cleanShares = sharesAmount.replaceAll("[,$\\s]", "");
                                holding.setShares(Long.parseLong(cleanShares));
                            } catch (NumberFormatException e) {
                                logger.debug("⚠️ 无法解析股数字段: " + sharesAmount);
                            }
                        }
                        
                        holdings.add(holding);
                        logger.debug("✅ 解析持仓记录: " + issuerName + " (CUSIP: " + cusip + ", 价值: " + value + ")");
                        
                    } else {
                        logger.debug("❌ 持仓记录缺少必需字段: " + issuerName + " | " + cusip + " | " + value);
                    }
                } catch (Exception e) {
                    logger.debug("❌ 解析单条持仓记录失败: " + e.getMessage());
                }
            }
            
            logger.debug("🔍 严格模式找到 " + foundCount + " 条记录，解析成功 " + holdings.size() + " 条");
            
            // 如果严格模式没有找到数据，尝试更宽松的模式
            if (holdings.isEmpty()) {
                logger.debug("🔄 严格模式未找到数据，尝试宽松模式...");
                holdings = parseWithRelaxedPattern(htmlContent);
            }
            
        } catch (Exception e) {
            logger.error("❌ 解析HTML表格失败: " + e.getMessage());
        }
        
        return holdings;
    }
    
    /**
     * 使用更宽松的模式解析HTML表格
     */
    private static List<Holding> parseWithRelaxedPattern(String htmlContent) {
        List<Holding> holdings = new ArrayList<>();
        logger.debug("🔄 尝试使用宽松模式解析HTML表格...");
        
        try {
            // 策略1: 逐个匹配各个数据字段，然后组合
            logger.debug("🔍 策略1: 分别提取各字段...");
            
            // 提取所有发行人名称
            Pattern issuerPattern = Pattern.compile("<td[^>]*class=\"FormData\"[^>]*>([A-Z][A-Z\\s&,.-]+)</td>", Pattern.CASE_INSENSITIVE);
            Matcher issuerMatcher = issuerPattern.matcher(htmlContent);
            
            List<String> issuers = new ArrayList<>();
            while (issuerMatcher.find()) {
                String issuer = cleanHtmlText(issuerMatcher.group(1));
                if (isValidData(issuer) && issuer.length() > 3) { // 排除太短的字符串
                    issuers.add(issuer);
                    logger.debug("  发行人: " + issuer);
                }
            }
            
            // 提取所有CUSIP代码
            Pattern cusipPattern = Pattern.compile("<td[^>]*class=\"FormData\"[^>]*>([A-Z0-9]{9})</td>", Pattern.CASE_INSENSITIVE);
            Matcher cusipMatcher = cusipPattern.matcher(htmlContent);
            
            List<String> cusips = new ArrayList<>();
            while (cusipMatcher.find()) {
                String cusip = cleanHtmlText(cusipMatcher.group(1));
                if (isValidData(cusip) && cusip.length() == 9) {
                    cusips.add(cusip);
                    logger.debug("  CUSIP: " + cusip);
                }
            }
            
            // 提取所有价值数据
            Pattern valuePattern = Pattern.compile("<td[^>]*class=\"FormDataR\"[^>]*>([0-9,]+)</td>", Pattern.CASE_INSENSITIVE);
            Matcher valueMatcher = valuePattern.matcher(htmlContent);
            
            List<String> values = new ArrayList<>();
            while (valueMatcher.find()) {
                String value = cleanHtmlText(valueMatcher.group(1));
                if (isValidData(value)) {
                    // 过滤掉太小的数值（可能是其他列的数据）
                    try {
                        long numericValue = Long.parseLong(value.replaceAll("[,$\\s]", ""));
                        if (numericValue > 1000) { // 价值应该大于1000（千美元）
                            values.add(value);
                            logger.debug("  价值: " + value);
                        }
                    } catch (NumberFormatException e) {
                        // 忽略无法解析的数值
                    }
                }
            }
            
            // 组合数据 - 假设它们在HTML中的出现顺序是对应的
            int minSize = Math.min(Math.min(issuers.size(), cusips.size()), values.size());
            logger.debug("🔗 组合数据: 发行人" + issuers.size() + "个, CUSIP" + cusips.size() + "个, 价值" + values.size() + "个, 最小" + minSize + "个");
            
            for (int i = 0; i < minSize; i++) {
                try {
                    Holding holding = new Holding();
                    holding.setNameOfIssuer(issuers.get(i));
                    holding.setCusip(cusips.get(i));
                    
                    String cleanValue = values.get(i).replaceAll("[,$\\s]", "");
                    holding.setValue(new BigDecimal(cleanValue));
                    
                    holdings.add(holding);
                    logger.debug("✅ 宽松模式组合: " + issuers.get(i) + " (CUSIP: " + cusips.get(i) + ", 价值: " + values.get(i) + ")");
                    
                } catch (Exception e) {
                    logger.debug("❌ 宽松模式组合失败 [" + i + "]: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            logger.error("❌ 宽松模式解析失败: " + e.getMessage());
        }
        
        logger.debug("🔄 宽松模式解析完成，找到 " + holdings.size() + " 条记录");
        return holdings;
    }
    
    /**
     * 清理HTML文本内容
     */
    private static String cleanHtmlText(String text) {
        if (text == null) return "";
        
        // 移除HTML标签和实体
        text = text.replaceAll("<[^>]+>", "");
        text = text.replaceAll("&nbsp;", " ");
        text = text.replaceAll("&amp;", "&");
        text = text.replaceAll("&lt;", "<");
        text = text.replaceAll("&gt;", ">");
        text = text.replaceAll("&quot;", "\"");
        text = text.replaceAll("&#39;", "'");
        text = text.replaceAll("Â", ""); // 移除常见的编码问题字符
        
        return text.trim();
    }
    
    /**
     * 验证数据是否有效（非空且非空白字符）
     */
    private static boolean isValidData(String data) {
        return data != null && !data.trim().isEmpty() && !data.trim().equals("Â") && !data.trim().equals(" ");
    }
    
    /**
     * 从HTML内容中提取报告期间
     * 查找表格标题或其他位置的期间信息
     */
    private static LocalDate extractReportPeriod(String htmlContent) {
        try {
            // 查找期间相关的文本
            Pattern[] periodPatterns = {
                Pattern.compile("PERIOD\\s+ENDING[:\\s]+(\\d{4}-\\d{2}-\\d{2})", Pattern.CASE_INSENSITIVE),
                Pattern.compile("PERIOD\\s+OF\\s+REPORT[:\\s]+(\\d{4}-\\d{2}-\\d{2})", Pattern.CASE_INSENSITIVE),
                Pattern.compile("REPORT\\s+DATE[:\\s]+(\\d{4}-\\d{2}-\\d{2})", Pattern.CASE_INSENSITIVE),
                Pattern.compile("CONFORMED\\s+PERIOD\\s+OF\\s+REPORT[:\\s]+(\\d{8})", Pattern.CASE_INSENSITIVE),
                Pattern.compile("FOR\\s+THE\\s+PERIOD\\s+ENDED[:\\s]+(\\d{4}-\\d{2}-\\d{2})", Pattern.CASE_INSENSITIVE)
            };
            
            for (Pattern pattern : periodPatterns) {
                Matcher matcher = pattern.matcher(htmlContent);
                if (matcher.find()) {
                    String dateStr = matcher.group(1).trim();
                    
                    // 处理不同的日期格式
                    if (dateStr.length() == 8 && dateStr.matches("\\d{8}")) {
                        // YYYYMMDD格式
                        String year = dateStr.substring(0, 4);
                        String month = dateStr.substring(4, 6);
                        String day = dateStr.substring(6, 8);
                        dateStr = year + "-" + month + "-" + day;
                    }
                    
                    try {
                        return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    } catch (Exception e) {
                        logger.debug("❌ 解析日期失败: " + dateStr);
                    }
                }
            }
            
        } catch (Exception e) {
            logger.debug("❌ 提取报告期间失败: " + e.getMessage());
        }
        
        return null;
    }
}