package com.company.sec13f.service.scraper;

import com.company.sec13f.repository.model.Filing;
import com.company.sec13f.repository.model.Holding;
import com.company.sec13f.service.parser.Enhanced13FXMLParser;
import com.company.sec13f.service.util.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.Closeable;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Real SEC scraper that retrieves actual 13F filings from SEC EDGAR database
 */
public class RealSECScraper implements Closeable {
    private static final String SEC_DATA_BASE_URL = "https://data.sec.gov";
    private static final String SEC_ARCHIVES_BASE_URL = "https://www.sec.gov/Archives";
    private static final long REQUEST_DELAY_MS = 100; // SEC文档要求：每秒不超过10次请求，即100ms间隔
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Logger logger;
    private long lastRequestTime = 0;

    public RealSECScraper() {
        this.httpClient = HttpClientBuilder.create()
            .setUserAgent("SEC13F Analysis Tool admin@sec13fparser.com") // 符合SEC文档要求的格式
            .build();
        this.objectMapper = new ObjectMapper();
        this.logger = Logger.getInstance();
    }

    /**
     * 获取公司的所有13F文件列表
     */
    public List<Filing> getCompanyFilings(String cik) throws IOException, InterruptedException {
        rateLimitRequest();
        
        String url = SEC_DATA_BASE_URL + "/submissions/CIK" + formatCik(cik) + ".json";
        logger.secRequest(url, 0);
        
        String jsonResponse = executeGetRequest(url);
        List<Filing> filings = new ArrayList<>();
        
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode recentFilings = root.path("filings").path("recent");
            
            JsonNode forms = recentFilings.path("form");
            JsonNode filingDates = recentFilings.path("filingDate");
            JsonNode accessionNumbers = recentFilings.path("accessionNumber");
            
            for (int i = 0; i < forms.size(); i++) {
                String form = forms.get(i).asText();
                if ("13F-HR".equals(form) || "13F-HR/A".equals(form)) {
                    Filing filing = new Filing();
                    filing.setCik(cik);
                    filing.setAccessionNumber(accessionNumbers.get(i).asText());
                    filing.setFilingDate(LocalDate.parse(filingDates.get(i).asText()));
                    filing.setFilingType(form);
                    filings.add(filing);
                }
            }
            
            logger.info("Found " + filings.size() + " 13F filings for CIK: " + cik);
            return filings;
            
        } catch (Exception e) {
            logger.error("Failed to parse SEC submissions data", e);
            throw new IOException("Failed to parse SEC data: " + e.getMessage());
        }
    }

    /**
     * 获取最新的13F文件
     */
    public Filing getLatest13F(String cik) throws IOException, InterruptedException {
        List<Filing> filings = getCompanyFilings(cik);
        if (filings.isEmpty()) {
            throw new IOException("No 13F filings found for CIK: " + cik);
        }
        
        // 返回最新的文件
        Filing latest = filings.get(0);
        return get13FDetails(latest.getAccessionNumber(), cik);
    }

    /**
     * 获取13F文件的详细信息和持仓数据
     */
    public Filing get13FDetails(String accessionNumber, String cik) throws IOException, InterruptedException {
        rateLimitRequest();
        
        String normalizedAccession = accessionNumber.replace("-", "");
        String baseUrl = SEC_ARCHIVES_BASE_URL + "/edgar/data/" + formatCik(cik) + "/" + normalizedAccession;
        
        // 根据SEC文档优先查找INFORMATION_TABLE相关文件
        String[] possibleFiles = {
            "/form13fInfoTable.xml",           // 标准的信息表文件
            "/" + accessionNumber + ".txt",   // 文本格式的完整文件
            "/informationTable.xml",          // 可能的信息表变体
            "/infoTable.xml",                 // 简化命名的信息表
            "/form13f.xml",                   // 通用13F文件
            "/primary_doc.xml"                // 主文档（通常只包含元数据）
        };
        
        // 先尝试获取目录列表，寻找其他XML文件
        try {
            String directoryUrl = baseUrl + "/";
            logger.secRequest(directoryUrl, 0);
            String directoryContent = executeGetRequest(directoryUrl);
            
            // 从目录列表中找到XML文件
            if (directoryContent != null) {
                logger.debug("Directory content length: " + directoryContent.length());
                java.util.regex.Pattern xmlPattern = java.util.regex.Pattern.compile("href=\"[^\"]*?/([^/\"]*\\.xml)\"");
                java.util.regex.Matcher xmlMatcher = xmlPattern.matcher(directoryContent);
                java.util.List<String> foundXmlFiles = new java.util.ArrayList<>();
                
                while (xmlMatcher.find()) {
                    String xmlFile = xmlMatcher.group(1);
                    logger.debug("Found XML file: " + xmlFile);
                    
                    // 优先添加包含INFORMATION_TABLE关键词的文件
                    if (xmlFile.toLowerCase().contains("info") || xmlFile.toLowerCase().contains("table")) {
                        foundXmlFiles.add(0, "/" + xmlFile); // 添加到列表开头，优先处理
                        logger.debug("Prioritized XML file (contains info/table): " + xmlFile);
                    } else if (!xmlFile.equals("primary_doc.xml")) {
                        foundXmlFiles.add("/" + xmlFile);
                        logger.debug("Added XML file to search list: " + xmlFile);
                    }
                }
                logger.debug("Total additional XML files found: " + foundXmlFiles.size());
                
                // 将发现的XML文件添加到搜索列表
                if (!foundXmlFiles.isEmpty()) {
                    String[] expandedFiles = new String[possibleFiles.length + foundXmlFiles.size()];
                    System.arraycopy(possibleFiles, 0, expandedFiles, 0, possibleFiles.length);
                    for (int i = 0; i < foundXmlFiles.size(); i++) {
                        expandedFiles[possibleFiles.length + i] = foundXmlFiles.get(i);
                    }
                    possibleFiles = expandedFiles;
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to get directory listing: " + e.getMessage());
        }
        
        // 第一轮：优先查找包含informationTable的文件（实际持仓数据）
        for (String fileName : possibleFiles) {
            try {
                String url = baseUrl + fileName;
                logger.secRequest(url, 0);
                String content = executeGetRequest(url);
                
                if (content != null && (content.contains("informationTable") || content.contains("infoTable"))) {
                    logger.info("Found holdings data in file: " + fileName + " (contains informationTable)");
                    Filing filing = parse13FContent(content, accessionNumber, cik);
                    if (filing != null) {
                        filing.setFormFile(fileName.substring(1)); // 移除开头的"/"
                        if (filing.getHoldings() != null && !filing.getHoldings().isEmpty()) {
                            logger.info("Successfully extracted " + filing.getHoldings().size() + " holdings from " + fileName);
                            return filing;
                        } else {
                            logger.debug("No holdings extracted from " + fileName + ", continuing search...");
                        }
                    }
                }
            } catch (Exception e) {
                logger.debug("Failed to fetch " + fileName + ": " + e.getMessage());
            }
        }
        
        // 第二轮：查找包含持仓字段的文件
        for (String fileName : possibleFiles) {
            try {
                String url = baseUrl + fileName;
                logger.secRequest(url, 0);
                String content = executeGetRequest(url);
                
                if (content != null && (content.contains("nameOfIssuer") || content.contains("cusip")) && content.contains("value")) {
                    logger.info("Found potential holdings data in file: " + fileName + " (contains holding fields)");
                    Filing filing = parse13FContent(content, accessionNumber, cik);
                    if (filing != null) {
                        filing.setFormFile(fileName.substring(1)); // 移除开头的"/"
                        if (filing.getHoldings() != null && !filing.getHoldings().isEmpty()) {
                            logger.info("Successfully extracted " + filing.getHoldings().size() + " holdings from " + fileName);
                            return filing;
                        }
                    }
                }
            } catch (Exception e) {
                logger.debug("Failed to fetch " + fileName + ": " + e.getMessage());
            }
        }
        
        // 第三轮：如果还没找到，查找其他13F相关文件作为最后尝试
        for (String fileName : possibleFiles) {
            try {
                String url = baseUrl + fileName;
                logger.secRequest(url, 0);
                String content = executeGetRequest(url);
                
                if (content != null && content.contains("13F")) {
                    logger.info("Found 13F metadata in file: " + fileName);
                    Filing filing = parse13FContent(content, accessionNumber, cik);
                    if (filing != null) {
                        filing.setFormFile(fileName.substring(1)); // 移除开头的"/"
                    }
                    return filing;
                }
            } catch (Exception e) {
                logger.debug("Failed to fetch " + fileName + ": " + e.getMessage());
            }
        }
        
        throw new IOException("Could not find valid 13F file for accession number: " + accessionNumber);
    }

    /**
     * 解析13F文件内容 - 使用增强的XML解析器
     */
    private Filing parse13FContent(String content, String accessionNumber, String cik) {
        return Enhanced13FXMLParser.parse13FContent(content, accessionNumber, cik);
    }

    /**
     * 限制请求频率以符合SEC要求
     */
    private void rateLimitRequest() throws InterruptedException {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastRequest = currentTime - lastRequestTime;
        
        if (timeSinceLastRequest < REQUEST_DELAY_MS) {
            TimeUnit.MILLISECONDS.sleep(REQUEST_DELAY_MS - timeSinceLastRequest);
        }
        
        lastRequestTime = System.currentTimeMillis();
    }

    /**
     * 格式化CIK为10位数字
     */
    private String formatCik(String cik) {
        String numericCik = cik.replaceAll("\\D", "");
        return String.format("%010d", Long.parseLong(numericCik));
    }

    /**
     * 执行HTTP GET请求
     */
    private String executeGetRequest(String url) throws IOException {
        HttpGet request = new HttpGet(url);
        request.setHeader("User-Agent", "SEC13F Analysis Tool admin@sec13fparser.com");
        request.setHeader("Accept-Encoding", "gzip, deflate");
        request.setHeader("Accept", "application/json, */*");
        
        // 根据URL设置适当的Host头
        if (url.contains("data.sec.gov")) {
            request.setHeader("Host", "data.sec.gov");
        } else if (url.contains("www.sec.gov")) {
            request.setHeader("Host", "www.sec.gov");
        }
        
        HttpResponse response = httpClient.execute(request);
        int statusCode = response.getStatusLine().getStatusCode();
        
        logger.secRequest(url, statusCode);
        
        if (statusCode != 200) {
            throw new IOException("SEC request failed with status: " + statusCode + " for URL: " + url);
        }
        
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            return EntityUtils.toString(entity);
        }
        
        return null;
    }

    @Override
    public void close() throws IOException {
        // Apache HttpClient 4.x doesn't implement Closeable
        // Connection pool cleanup is handled automatically
    }
}