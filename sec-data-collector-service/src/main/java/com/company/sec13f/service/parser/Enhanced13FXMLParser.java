package com.company.sec13f.service.parser;

import com.company.sec13f.repository.model.Filing;
import com.company.sec13f.repository.model.Holding;
import com.company.sec13f.service.util.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 增强版13F XML解析器，支持多种XML格式
 */
public class Enhanced13FXMLParser {
    
    private static final Logger logger = Logger.getInstance();
    
    // 多种日期格式
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("MM-dd-yyyy"),
        DateTimeFormatter.ofPattern("yyyyMMdd")
    };
    
    /**
     * 解析13F XML内容
     */
    public static Filing parse13FContent(String content, String accessionNumber, String cik) {
        Filing filing = new Filing();
        filing.setAccessionNumber(accessionNumber);
        filing.setCik(cik);
        filing.setFilingType("13F-HR");
        
        List<Holding> holdings = new ArrayList<>();
        
        // 尝试DOM解析
        try {
            Document doc = parseXMLDocument(content);
            if (doc != null) {
                // 提取Filing Date
                LocalDate filingDate = extractFilingDate(doc, content);
                if (filingDate != null) {
                    filing.setFilingDate(filingDate);
                }
                
                // 解析持仓信息
                holdings = parseHoldingsFromDOM(doc);
                logger.info("DOM parsing extracted " + holdings.size() + " holdings");
            }
        } catch (Exception e) {
            logger.debug("DOM parsing failed, falling back to regex: " + e.getMessage());
        }
        
        // 如果DOM解析失败或没有找到持仓，使用增强的正则表达式解析
        if (holdings.isEmpty()) {
            holdings = parseHoldingsWithRegex(content);
            logger.info("Regex parsing extracted " + holdings.size() + " holdings");
            
            // 如果还是没有日期，尝试从内容中提取
            if (filing.getFilingDate() == null) {
                LocalDate filingDate = extractFilingDateWithRegex(content);
                if (filingDate != null) {
                    filing.setFilingDate(filingDate);
                }
            }
        }
        
        filing.setHoldings(holdings);
        logger.info("Total parsed " + holdings.size() + " holdings from 13F filing " + accessionNumber);
        
        return filing;
    }
    
    /**
     * 解析XML文档
     */
    private static Document parseXMLDocument(String content) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setIgnoringElementContentWhitespace(true);
            
            // 禁用DTD验证以避免DTD解析问题
            factory.setValidating(false);
            factory.setFeature("http://xml.org/sax/features/validation", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            
            DocumentBuilder builder = factory.newDocumentBuilder();
            // 设置错误处理器以忽略DTD警告
            builder.setErrorHandler(null);
            
            // 清理内容，移除可能的BOM和无效字符
            String cleanContent = cleanXMLContent(content);
            return builder.parse(new ByteArrayInputStream(cleanContent.getBytes("UTF-8")));
        } catch (Exception e) {
            logger.debug("Failed to parse XML document: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 清理XML内容
     */
    private static String cleanXMLContent(String content) {
        // 移除BOM
        if (content.startsWith("\uFEFF")) {
            content = content.substring(1);
        }
        
        // 移除或替换DTD声明以避免解析错误
        content = content.replaceAll("<!DOCTYPE[^>]*>", "");
        
        // 移除HTML DTD引用
        content = content.replaceAll("<!DOCTYPE\\s+[^>]*\"[^\"]*\\.dtd\"[^>]*>", "");
        
        // 移除可能导致问题的实体声明
        content = content.replaceAll("<!ENTITY[^>]*>", "");
        
        // 确保有XML声明
        if (!content.trim().startsWith("<?xml")) {
            content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + content;
        }
        
        return content;
    }
    
    /**
     * 从DOM解析持仓信息 - 优化版本，符合SEC标准格式
     */
    private static List<Holding> parseHoldingsFromDOM(Document doc) {
        List<Holding> holdings = new ArrayList<>();
        
        // 首先尝试标准的informationTable格式 (根据SEC API指南)
        NodeList infoTableNodes = doc.getElementsByTagName("infoTable");
        if (infoTableNodes.getLength() > 0) {
            logger.debug("🎯 找到标准的infoTable节点: " + infoTableNodes.getLength() + " 个");
            for (int i = 0; i < infoTableNodes.getLength(); i++) {
                Element infoTableElement = (Element) infoTableNodes.item(i);
                Holding holding = parseStandardInfoTable(infoTableElement);
                if (holding != null) {
                    holdings.add(holding);
                }
            }
            return holdings;
        }
        
        // 回退：尝试其他可能的元素名称和路径
        String[] holdingPaths = {
            "informationTable", 
            "ns1:infoTable",
            "ns1:informationTable",
            "holdingInfo",
            "holding"
        };
        
        for (String path : holdingPaths) {
            NodeList holdingNodes = doc.getElementsByTagName(path);
            
            if (holdingNodes != null && holdingNodes.getLength() > 0) {
                logger.debug("Found holdings using path: " + path);
                
                for (int i = 0; i < holdingNodes.getLength(); i++) {
                    try {
                        Element holdingElement = (Element) holdingNodes.item(i);
                        Holding holding = parseHoldingElement(holdingElement);
                        if (holding != null) {
                            holdings.add(holding);
                        }
                    } catch (Exception e) {
                        logger.debug("Failed to parse holding element " + i + ": " + e.getMessage());
                    }
                }
                break; // 找到数据就停止尝试其他路径
            }
        }
        
        return holdings;
    }
    
    /**
     * 解析单个持仓元素
     */
    private static Holding parseHoldingElement(Element element) {
        try {
            Holding holding = new Holding();
            
            // 尝试多种可能的字段名
            String issuer = getElementTextByNames(element, "nameOfIssuer", "issuer", "issuerName", "name");
            String cusip = getElementTextByNames(element, "cusip", "cusipNum", "cusipNumber");
            String value = getElementTextByNames(element, "value", "marketValue", "mktVal");
            
            // 处理新的SEC格式中的shares结构 - shrsOrPrnAmt/sshPrnamt
            String shares = getElementTextByNames(element, "sshPrnamt", "sharesOrPrincipalAmount", "shares", "amount", "sshPrn");
            if (shares == null) {
                // 尝试新的嵌套结构
                Element shrsElement = getFirstElementByNames(element, "shrsOrPrnAmt");
                if (shrsElement != null) {
                    shares = getElementTextByNames(shrsElement, "sshPrnamt", "sshPrnAmt");
                }
            }
            
            if (issuer == null || cusip == null) {
                logger.debug("Skipping holding - missing required fields");
                return null;
            }
            
            holding.setNameOfIssuer(issuer.trim());
            holding.setCusip(cusip.trim());
            
            // 解析数值，处理可能的千分位分隔符
            if (value != null && !value.trim().isEmpty()) {
                try {
                    String cleanValue = value.replaceAll("[,$\\s]", "");
                    holding.setValue(new BigDecimal(cleanValue));
                } catch (NumberFormatException e) {
                    logger.debug("Failed to parse value: " + value);
                }
            }
            
            if (shares != null && !shares.trim().isEmpty()) {
                try {
                    String cleanShares = shares.replaceAll("[,$\\s]", "");
                    holding.setShares(Long.parseLong(cleanShares));
                } catch (NumberFormatException e) {
                    logger.debug("Failed to parse shares: " + shares);
                }
            }
            
            return holding;
        } catch (Exception e) {
            logger.debug("Failed to parse holding element: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 根据多个可能的名称获取元素文本
     */
    private static String getElementTextByNames(Element parent, String... names) {
        for (String name : names) {
            NodeList nodes = parent.getElementsByTagName(name);
            if (nodes.getLength() > 0) {
                String text = nodes.item(0).getTextContent();
                if (text != null && !text.trim().isEmpty()) {
                    return text;
                }
            }
        }
        return null;
    }
    
    /**
     * 根据多个可能的名称获取第一个元素
     */
    private static Element getFirstElementByNames(Element parent, String... names) {
        for (String name : names) {
            NodeList nodes = parent.getElementsByTagName(name);
            if (nodes.getLength() > 0) {
                return (Element) nodes.item(0);
            }
        }
        return null;
    }
    
    /**
     * 使用增强的正则表达式解析持仓
     */
    private static List<Holding> parseHoldingsWithRegex(String content) {
        List<Holding> holdings = new ArrayList<>();
        
        // 多种正则表达式模式以匹配不同格式
        Pattern[] patterns = {
            // 标准格式
            Pattern.compile("<nameOfIssuer>\\s*(.*?)\\s*</nameOfIssuer>.*?<cusip>\\s*(.*?)\\s*</cusip>.*?<value>\\s*(.*?)\\s*</value>.*?<sshPrnamt>\\s*(.*?)\\s*</sshPrnamt>", Pattern.DOTALL),
            
            // 变体格式1
            Pattern.compile("<issuer>\\s*(.*?)\\s*</issuer>.*?<cusip>\\s*(.*?)\\s*</cusip>.*?<marketValue>\\s*(.*?)\\s*</marketValue>.*?<shares>\\s*(.*?)\\s*</shares>", Pattern.DOTALL),
            
            // 变体格式2
            Pattern.compile("<name>\\s*(.*?)\\s*</name>.*?<cusipNum>\\s*(.*?)\\s*</cusipNum>.*?<mktVal>\\s*(.*?)\\s*</mktVal>.*?<sshPrn>\\s*(.*?)\\s*</sshPrn>", Pattern.DOTALL),
            
            // 更宽松的格式
            Pattern.compile("(?i)<(?:nameofissuer|issuer|name)>\\s*(.*?)\\s*</(?:nameofissuer|issuer|name)>.*?<(?:cusip|cusipnum)>\\s*(.*?)\\s*</(?:cusip|cusipnum)>.*?<(?:value|marketvalue|mktval)>\\s*(.*?)\\s*</(?:value|marketvalue|mktval)>.*?<(?:sshprnamt|shares|amount|sshprn)>\\s*(.*?)\\s*</(?:sshprnamt|shares|amount|sshprn)>", Pattern.DOTALL)
        };
        
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(content);
            while (matcher.find()) {
                try {
                    Holding holding = new Holding();
                    holding.setNameOfIssuer(matcher.group(1).trim());
                    holding.setCusip(matcher.group(2).trim());
                    
                    // 清理并解析数值
                    String valueStr = matcher.group(3).trim().replaceAll("[,$\\s]", "");
                    String sharesStr = matcher.group(4).trim().replaceAll("[,$\\s]", "");
                    
                    if (!valueStr.isEmpty()) {
                        holding.setValue(new BigDecimal(valueStr));
                    }
                    if (!sharesStr.isEmpty()) {
                        holding.setShares(Long.parseLong(sharesStr));
                    }
                    
                    holdings.add(holding);
                } catch (Exception e) {
                    logger.debug("Failed to parse holding with regex: " + e.getMessage());
                }
            }
            
            if (!holdings.isEmpty()) {
                logger.debug("Successfully parsed holdings with pattern " + (pattern.pattern().length() > 50 ? 
                    pattern.pattern().substring(0, 50) + "..." : pattern.pattern()));
                break;
            }
        }
        
        return holdings;
    }
    
    /**
     * 从DOM提取Filing Date
     */
    private static LocalDate extractFilingDate(Document doc, String content) {
        // 尝试从XML元素中提取日期
        String[] dateTags = {"filingDate", "reportDate", "periodOfReport", "date", "asOfDate"};
        
        for (String tag : dateTags) {
            NodeList nodes = doc.getElementsByTagName(tag);
            if (nodes.getLength() > 0) {
                String dateText = nodes.item(0).getTextContent();
                LocalDate date = parseDate(dateText);
                if (date != null) {
                    return date;
                }
            }
        }
        
        return null;
    }
    
    /**
     * 解析标准的infoTable格式（符合SEC API指南）
     */
    private static Holding parseStandardInfoTable(Element infoTableElement) {
        try {
            Holding holding = new Holding();
            
            // 根据SEC API指南解析标准字段
            String nameOfIssuer = getElementTextContent(infoTableElement, "nameOfIssuer");
            String cusip = getElementTextContent(infoTableElement, "cusip");
            String valueStr = getElementTextContent(infoTableElement, "value");
            
            // 解析股份数量 - 从shrsOrPrnAmt/sshPrnamt节点获取
            String sharesStr = null;
            NodeList shrsOrPrnAmtNodes = infoTableElement.getElementsByTagName("shrsOrPrnAmt");
            if (shrsOrPrnAmtNodes.getLength() > 0) {
                Element shrsOrPrnAmtElement = (Element) shrsOrPrnAmtNodes.item(0);
                sharesStr = getElementTextContent(shrsOrPrnAmtElement, "sshPrnamt");
            }
            
            // 验证必需字段
            if (nameOfIssuer == null || cusip == null || valueStr == null) {
                logger.debug("❌ 标准infoTable缺少必需字段: nameOfIssuer=" + nameOfIssuer + 
                           ", cusip=" + cusip + ", value=" + valueStr);
                return null;
            }
            
            // 设置基本字段
            holding.setNameOfIssuer(nameOfIssuer.trim());
            holding.setCusip(cusip.trim());
            
            // 解析数值字段
            try {
                BigDecimal value = new BigDecimal(valueStr.replaceAll("[^0-9.-]", ""));
                holding.setValue(value);
            } catch (NumberFormatException e) {
                logger.debug("⚠️ 无法解析value字段: " + valueStr);
                return null;
            }
            
            // 解析股份数量
            if (sharesStr != null && !sharesStr.trim().isEmpty()) {
                try {
                    Long shares = Long.parseLong(sharesStr.replaceAll("[^0-9]", ""));
                    holding.setShares(shares);
                } catch (NumberFormatException e) {
                    logger.debug("⚠️ 无法解析shares字段: " + sharesStr);
                    // shares字段不是必需的，可以为空
                }
            }
            
            logger.debug("✅ 成功解析标准infoTable: " + nameOfIssuer + " (CUSIP: " + cusip + 
                        ", Value: " + valueStr + ", Shares: " + sharesStr + ")");
            return holding;
            
        } catch (Exception e) {
            logger.debug("❌ 解析标准infoTable失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 获取元素的文本内容
     */
    private static String getElementTextContent(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            String content = nodeList.item(0).getTextContent();
            return content != null ? content.trim() : null;
        }
        return null;
    }
    
    /**
     * 使用正则表达式提取Filing Date
     */
    private static LocalDate extractFilingDateWithRegex(String content) {
        Pattern[] datePatterns = {
            Pattern.compile("<filingDate>\\s*(.*?)\\s*</filingDate>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<reportDate>\\s*(.*?)\\s*</reportDate>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<periodOfReport>\\s*(.*?)\\s*</periodOfReport>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<date>\\s*(.*?)\\s*</date>", Pattern.CASE_INSENSITIVE)
        };
        
        for (Pattern pattern : datePatterns) {
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                String dateText = matcher.group(1).trim();
                LocalDate date = parseDate(dateText);
                if (date != null) {
                    return date;
                }
            }
        }
        
        // 如果找不到日期，使用当前日期的前一个季度末
        return LocalDate.now().minusMonths(3);
    }
    
    /**
     * 解析日期字符串
     */
    private static LocalDate parseDate(String dateText) {
        if (dateText == null || dateText.trim().isEmpty()) {
            return null;
        }
        
        dateText = dateText.trim();
        
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(dateText, formatter);
            } catch (DateTimeParseException e) {
                // 尝试下一个格式
            }
        }
        
        // 尝试提取数字格式的日期
        Pattern numberPattern = Pattern.compile("(\\d{4})(\\d{2})(\\d{2})");
        Matcher matcher = numberPattern.matcher(dateText.replaceAll("\\D", ""));
        if (matcher.find() && matcher.group().length() >= 8) {
            try {
                int year = Integer.parseInt(matcher.group().substring(0, 4));
                int month = Integer.parseInt(matcher.group().substring(4, 6));
                int day = Integer.parseInt(matcher.group().substring(6, 8));
                return LocalDate.of(year, month, day);
            } catch (Exception e) {
                // 忽略
            }
        }
        
        logger.debug("Failed to parse date: " + dateText);
        return null;
    }
}