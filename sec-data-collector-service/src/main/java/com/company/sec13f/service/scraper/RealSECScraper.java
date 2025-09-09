package com.company.sec13f.service.scraper;

import com.company.sec13f.repository.model.Filing;
import com.company.sec13f.repository.model.Holding;
import com.company.sec13f.service.parser.Enhanced13FXMLParser;
import com.company.sec13f.service.parser.HTML13FParser;
import com.company.sec13f.service.util.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
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
    private static final String SEC_SEARCH_API_URL = "https://efts.sec.gov/LATEST/search-index";
    private static final long REQUEST_DELAY_MS = 100; // SEC文档要求：每秒不超过10次请求，即100ms间隔
    
    private final CloseableHttpClient httpClient;
    private final PoolingHttpClientConnectionManager connectionManager;
    private final ObjectMapper objectMapper;
    private final Logger logger;
    private long lastRequestTime = 0;

    public RealSECScraper() {
        // 创建连接池管理器
        this.connectionManager = new PoolingHttpClientConnectionManager();
        this.connectionManager.setMaxTotal(20);                    // 最大连接数
        this.connectionManager.setDefaultMaxPerRoute(10);         // 每个路由的最大连接数
        
        // 配置请求超时
        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectionRequestTimeout(5000)   // 从连接池获取连接的超时时间 (5秒)
            .setConnectTimeout(10000)           // 建立连接的超时时间 (10秒)
            .setSocketTimeout(15000)            // 数据传输的超时时间 (15秒)
            .build();
            
        this.httpClient = HttpClientBuilder.create()
            .setConnectionManager(connectionManager)
            .setUserAgent("SEC13F Analysis Tool admin@sec13fparser.com") // 符合SEC文档要求的格式
            .setDefaultRequestConfig(requestConfig)
            .disableRedirectHandling()          // 禁用自动重定向
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
     * 获取13F文件的详细信息和持仓数据 - 优化版本，一次解析获取所有信息
     */
    public Filing get13FDetails(String accessionNumber, String cik) throws IOException, InterruptedException {
        rateLimitRequest();
        
        String normalizedAccession = accessionNumber.replace("-", "");
        String baseUrl = SEC_ARCHIVES_BASE_URL + "/edgar/data/" + formatCik(cik) + "/" + normalizedAccession;
        
        // 获取标准的13F提交文件(.txt格式) - 包含完整信息
        String submissionFileUrl = baseUrl + "/" + accessionNumber + ".txt";
        logger.debug("📄 获取13F提交文件: " + submissionFileUrl);
        
        try {
            String submissionContent = executeGetRequest(submissionFileUrl);
            
            if (submissionContent != null) {
                // 从SEC-DOCUMENT节点提取form_file
                String formFile = extractFormFileFromSecDocument(submissionContent);
                
                // 从提交文件头部提取EFFECTIVENESS DATE作为filingDate
                LocalDate effectivenessDate = extractEffectivenessDate(submissionContent);
                
                // 从提交文件头部提取CONFORMED PERIOD OF REPORT作为reportPeriod
                LocalDate reportPeriod = extractConformedPeriodOfReport(submissionContent);
                
                // 直接从提交文件中解析Information Table部分的持仓信息
                String informationTableXml = extractInformationTableContent(submissionContent);
                
                if (informationTableXml != null && !informationTableXml.trim().isEmpty()) {
                    logger.info("✅ 直接从提交文件中提取到Information Table内容");
                    // 解析XML内容获取持仓数据，使用提取的生效日期、报告期间和form_file
                    Filing filing = parse13FContentWithDateAndReportPeriod(informationTableXml, accessionNumber, cik, effectivenessDate, reportPeriod, formFile);
                    if (filing != null && filing.getHoldings() != null && !filing.getHoldings().isEmpty()) {
                        logger.info("📈 成功解析 " + filing.getHoldings().size() + " 条持仓记录");
                        return filing;
                    }
                } else {
                    logger.warn("⚠️ 在提交文件中未找到Information Table内容，回退到传统方法");
                }
            }
        } catch (Exception e) {
            logger.warn("⚠️ 解析提交文件失败，回退到传统方法: " + e.getMessage());
        }
        
        // 回退方案：使用原有的文件搜索逻辑
        return get13FDetailsLegacy(accessionNumber, cik, baseUrl);
    }

    /**
     * 从13F提交文件中直接提取Information Table的XML内容
     */
    private String extractInformationTableContent(String submissionContent) {
        try {
            // 查找INFORMATION TABLE类型的文档
            String[] lines = submissionContent.split("\n");
            boolean inInfoTableDocument = false;
            boolean inTextSection = false;
            boolean inXmlSection = false;
            StringBuilder xmlContent = new StringBuilder();
            
            for (String line : lines) {
                String trimmedLine = line.trim();
                
                // 检测INFORMATION TABLE文档的开始
                if (trimmedLine.equals("<TYPE>INFORMATION TABLE")) {
                    inInfoTableDocument = true;
                    logger.debug("🔍 找到INFORMATION TABLE文档部分");
                    continue;
                }
                
                // 如果在INFORMATION TABLE文档中
                if (inInfoTableDocument) {
                    // 检测TEXT部分的开始
                    if (trimmedLine.equals("<TEXT>")) {
                        inTextSection = true;
                        logger.debug("🔍 找到TEXT部分");
                        continue;
                    }
                    
                    // 如果在TEXT部分中
                    if (inTextSection) {
                        // 检测XML部分的开始
                        if (trimmedLine.equals("<XML>")) {
                            inXmlSection = true;
                            logger.debug("🔍 找到XML部分");
                            continue;
                        }
                        
                        // 检测XML部分的结束
                        if (trimmedLine.equals("</XML>")) {
                            inXmlSection = false;
                            logger.debug("✅ XML部分结束，提取到 " + xmlContent.length() + " 个字符");
                            break;
                        }
                        
                        // 如果在XML部分中，收集XML内容
                        if (inXmlSection) {
                            xmlContent.append(line).append("\n");
                        }
                    }
                    
                    // 遇到新的文档类型时退出
                    if (trimmedLine.startsWith("<TYPE>") && !trimmedLine.equals("<TYPE>INFORMATION TABLE")) {
                        break;
                    }
                }
            }
            
            String result = xmlContent.toString().trim();
            if (!result.isEmpty()) {
                logger.debug("📊 成功提取Information Table XML内容，长度: " + result.length() + " 字符");
                return result;
            }
            
        } catch (Exception e) {
            logger.debug("❌ 解析提交文件时出错: " + e.getMessage());
        }
        
        return null;
    }

    /**
     * 从13F提交文件头部提取EFFECTIVENESS DATE作为filingDate
     */
    private LocalDate extractEffectivenessDate(String submissionContent) {
        try {
            String[] lines = submissionContent.split("\n");
            
            for (String line : lines) {
                String trimmedLine = line.trim();
                
                // 查找EFFECTIVENESS DATE字段
                if (trimmedLine.startsWith("EFFECTIVENESS DATE:")) {
                    String dateStr = trimmedLine.substring("EFFECTIVENESS DATE:".length()).trim();
                    
                    try {
                        // SEC日期格式通常为 YYYYMMDD
                        if (dateStr.length() == 8 && dateStr.matches("\\d{8}")) {
                            LocalDate effectivenessDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
                            logger.debug("📅 提取到EFFECTIVENESS DATE: " + effectivenessDate);
                            return effectivenessDate;
                        }
                        
                        // 如果是其他格式，尝试YYYY-MM-DD
                        if (dateStr.length() == 10 && dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                            LocalDate effectivenessDate = LocalDate.parse(dateStr);
                            logger.debug("📅 提取到EFFECTIVENESS DATE: " + effectivenessDate);
                            return effectivenessDate;
                        }
                        
                    } catch (Exception e) {
                        logger.debug("❌ 解析EFFECTIVENESS DATE失败: " + dateStr + " - " + e.getMessage());
                    }
                    break;
                }
                
                // 如果遇到DOCUMENT开始，说明已经跳出了头部区域
                if (trimmedLine.equals("<DOCUMENT>")) {
                    break;
                }
            }
            
        } catch (Exception e) {
            logger.debug("❌ 提取EFFECTIVENESS DATE时出错: " + e.getMessage());
        }
        
        logger.debug("⚠️ 未找到EFFECTIVENESS DATE，将使用默认日期");
        return null;
    }

    /**
     * 从SEC-DOCUMENT节点提取form_file
     */
    private String extractFormFileFromSecDocument(String submissionContent) {
        try {
            String[] lines = submissionContent.split("\n");
            
            for (String line : lines) {
                String trimmedLine = line.trim();
                
                // 查找SEC-DOCUMENT节点: <SEC-DOCUMENT>filename.txt : timestamp
                if (trimmedLine.startsWith("<SEC-DOCUMENT>")) {
                    String content = trimmedLine.substring("<SEC-DOCUMENT>".length()).trim();
                    
                    // 提取文件名部分（在" : "之前）
                    int colonIndex = content.indexOf(" : ");
                    if (colonIndex > 0) {
                        String fileName = content.substring(0, colonIndex).trim();
                        logger.debug("📄 从SEC-DOCUMENT节点提取到form_file: " + fileName);
                        return fileName;
                    } else {
                        // 如果没有时间戳部分，整个内容就是文件名
                        logger.debug("📄 从SEC-DOCUMENT节点提取到form_file: " + content);
                        return content;
                    }
                }
            }
            
        } catch (Exception e) {
            logger.debug("❌ 提取form_file时出错: " + e.getMessage());
        }
        
        logger.debug("⚠️ 未找到SEC-DOCUMENT节点，将使用默认form_file");
        return null;
    }

    /**
     * 从13F提交文件头部提取CONFORMED PERIOD OF REPORT作为reportPeriod
     */
    private LocalDate extractConformedPeriodOfReport(String submissionContent) {
        try {
            String[] lines = submissionContent.split("\n");
            
            for (String line : lines) {
                String trimmedLine = line.trim();
                
                // 查找CONFORMED PERIOD OF REPORT字段
                if (trimmedLine.startsWith("CONFORMED PERIOD OF REPORT:")) {
                    String dateStr = trimmedLine.substring("CONFORMED PERIOD OF REPORT:".length()).trim();
                    
                    try {
                        // SEC日期格式通常为 YYYYMMDD
                        if (dateStr.length() == 8 && dateStr.matches("\\d{8}")) {
                            LocalDate reportPeriod = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
                            logger.debug("📅 提取到CONFORMED PERIOD OF REPORT: " + reportPeriod);
                            return reportPeriod;
                        }
                        
                        // 如果是其他格式，尝试YYYY-MM-DD
                        if (dateStr.length() == 10 && dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                            LocalDate reportPeriod = LocalDate.parse(dateStr);
                            logger.debug("📅 提取到CONFORMED PERIOD OF REPORT: " + reportPeriod);
                            return reportPeriod;
                        }
                        
                    } catch (Exception e) {
                        logger.debug("❌ 解析CONFORMED PERIOD OF REPORT失败: " + dateStr + " - " + e.getMessage());
                    }
                    break;
                }
                
                // 如果遇到DOCUMENT开始，说明已经跳出了头部区域
                if (trimmedLine.equals("<DOCUMENT>")) {
                    break;
                }
            }
            
        } catch (Exception e) {
            logger.debug("❌ 提取CONFORMED PERIOD OF REPORT时出错: " + e.getMessage());
        }
        
        logger.debug("⚠️ 未找到CONFORMED PERIOD OF REPORT，将使用默认日期");
        return null;
    }

    /**
     * 传统的13F文件搜索方法（作为回退方案）
     */
    private Filing get13FDetailsLegacy(String accessionNumber, String cik, String baseUrl) 
            throws IOException, InterruptedException {
        logger.info("🔄 使用传统方法搜索13F文件...");
        
        // 根据SEC文档优先查找INFORMATION_TABLE相关文件
        String[] possibleFiles = {
            "/form13fInfoTable.xml",           // 标准的信息表文件
            "/informationTable.xml",          // 可能的信息表变体
            "/infoTable.xml",                 // 简化命名的信息表
            "/form13f.xml",                   // 通用13F文件
            "/primary_doc.xml"                // 主文档（通常只包含元数据）
        };
        
        // 优先查找包含informationTable的文件（实际持仓数据）
        for (String fileName : possibleFiles) {
            try {
                String url = baseUrl + fileName;
                String content = executeGetRequest(url);
                
                if (content != null && (content.contains("informationTable") || content.contains("infoTable"))) {
                    logger.info("✅ 在传统文件中找到持仓数据: " + fileName);
                    Filing filing = parse13FContent(content, accessionNumber, cik);
                    if (filing != null) {
                        filing.setFormFile(fileName.substring(1)); // 移除开头的"/"
                        if (filing.getHoldings() != null && !filing.getHoldings().isEmpty()) {
                            logger.info("📈 成功解析 " + filing.getHoldings().size() + " 条持仓记录");
                            return filing;
                        }
                    }
                }
            } catch (Exception e) {
                logger.debug("❌ 尝试文件失败 " + fileName + ": " + e.getMessage());
            }
        }
        
        throw new IOException("无法找到有效的13F持仓数据文件，AccessionNumber: " + accessionNumber);
    }

    /**
     * 解析13F文件内容 - 自动检测格式并使用合适的解析器
     */
    private Filing parse13FContent(String content, String accessionNumber, String cik) {
        // 检测内容格式（HTML vs XML）
        if (isHTMLFormat(content)) {
            logger.info("🌐 检测到HTML格式，使用HTML解析器");
            return HTML13FParser.parseHTML13FContent(content, accessionNumber, cik);
        } else {
            logger.info("📄 检测到XML格式，使用XML解析器");
            return Enhanced13FXMLParser.parse13FContent(content, accessionNumber, cik);
        }
    }

    /**
     * 解析13F文件内容 - 自动检测格式并设置指定的生效日期
     */
    private Filing parse13FContentWithDate(String content, String accessionNumber, String cik, LocalDate effectivenessDate) {
        Filing filing;
        
        // 检测内容格式并使用合适的解析器
        if (isHTMLFormat(content)) {
            logger.info("🌐 检测到HTML格式，使用HTML解析器");
            filing = HTML13FParser.parseHTML13FContent(content, accessionNumber, cik);
        } else {
            logger.info("📄 检测到XML格式，使用XML解析器");
            filing = Enhanced13FXMLParser.parse13FContent(content, accessionNumber, cik);
        }
        
        // 如果提取到了生效日期，则使用它作为filing date
        if (filing != null && effectivenessDate != null) {
            filing.setFilingDate(effectivenessDate);
            logger.debug("✅ 使用EFFECTIVENESS DATE作为filing date: " + effectivenessDate);
        }
        
        return filing;
    }

    /**
     * 解析13F文件内容 - 自动检测格式并设置指定的生效日期和form_file
     */
    private Filing parse13FContentWithDateAndFile(String content, String accessionNumber, String cik, 
                                                  LocalDate effectivenessDate, String formFile) {
        Filing filing;
        
        // 检测内容格式并使用合适的解析器
        if (isHTMLFormat(content)) {
            logger.info("🌐 检测到HTML格式，使用HTML解析器");
            filing = HTML13FParser.parseHTML13FContent(content, accessionNumber, cik);
        } else {
            logger.info("📄 检测到XML格式，使用XML解析器");
            filing = Enhanced13FXMLParser.parse13FContent(content, accessionNumber, cik);
        }
        
        if (filing != null) {
            // 如果提取到了生效日期，则使用它作为filing date
            if (effectivenessDate != null) {
                filing.setFilingDate(effectivenessDate);
                logger.debug("✅ 使用EFFECTIVENESS DATE作为filing date: " + effectivenessDate);
            }
            
            // 如果提取到了form_file，则设置它
            if (formFile != null && !formFile.trim().isEmpty()) {
                filing.setFormFile(formFile);
                logger.debug("✅ 从SEC-DOCUMENT节点设置form_file: " + formFile);
            }
        }
        
        return filing;
    }

    /**
     * 解析13F文件内容 - 使用增强的XML解析器，并设置指定的生效日期、报告期间和form_file
     */
    private Filing parse13FContentWithDateAndReportPeriod(String content, String accessionNumber, String cik, 
                                                          LocalDate effectivenessDate, LocalDate reportPeriod, String formFile) {
        Filing filing;
        
        // 检测内容格式并使用合适的解析器
        if (isHTMLFormat(content)) {
            logger.info("🌐 检测到HTML格式，使用HTML解析器");
            filing = HTML13FParser.parseHTML13FContent(content, accessionNumber, cik);
        } else {
            logger.info("📄 检测到XML格式，使用XML解析器");
            filing = Enhanced13FXMLParser.parse13FContent(content, accessionNumber, cik);
        }
        
        if (filing != null) {
            // 如果提取到了生效日期，则使用它作为filing date
            if (effectivenessDate != null) {
                filing.setFilingDate(effectivenessDate);
                logger.debug("✅ 使用EFFECTIVENESS DATE作为filing date: " + effectivenessDate);
            }
            
            // 如果提取到了报告期间，则设置它
            if (reportPeriod != null) {
                filing.setReportPeriod(reportPeriod);
                logger.debug("✅ 使用CONFORMED PERIOD OF REPORT作为report period: " + reportPeriod);
            }
            
            // 如果提取到了form_file，则设置它
            if (formFile != null && !formFile.trim().isEmpty()) {
                filing.setFormFile(formFile);
                logger.debug("✅ 从SEC-DOCUMENT节点设置form_file: " + formFile);
            }
        }
        
        return filing;
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
        
        // 在调试模式下显示连接池状态
        if (logger != null) {
            logConnectionPoolStatus();
        }
    }
    
    /**
     * 记录连接池状态（用于调试）
     */
    private void logConnectionPoolStatus() {
        if (connectionManager != null) {
            try {
                int totalStats = connectionManager.getTotalStats().getAvailable() + 
                                connectionManager.getTotalStats().getLeased();
                int available = connectionManager.getTotalStats().getAvailable();
                int leased = connectionManager.getTotalStats().getLeased();
                int pending = connectionManager.getTotalStats().getPending();
                
                logger.debug("📦 连接池状态: 总连接=" + totalStats + ", 可用=" + available + 
                           ", 已租用=" + leased + ", 等待=" + pending);
            } catch (Exception e) {
                // 忽略连接池状态查询错误
            }
        }
    }

    /**
     * 格式化CIK为10位数字
     */
    private String formatCik(String cik) {
        String numericCik = cik.replaceAll("\\D", "");
        return String.format("%010d", Long.parseLong(numericCik));
    }

    /**
     * 执行HTTP GET请求 (改进版本，确保连接正确释放)
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
        
        long startTime = System.currentTimeMillis();
        
        // 使用try-with-resources确保响应正确关闭
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            long duration = System.currentTimeMillis() - startTime;
            int statusCode = response.getStatusLine().getStatusCode();
            
            logger.secRequest(url, statusCode);
            logger.debug("⏱️ 请求完成，耗时: " + duration + "ms, 状态码: " + statusCode);
            
            if (statusCode != 200) {
                // 消耗实体内容以释放连接
                EntityUtils.consumeQuietly(response.getEntity());
                throw new IOException("SEC request failed with status: " + statusCode + " for URL: " + url);
            }
            
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                try {
                    String content = EntityUtils.toString(entity, "UTF-8");
                    logger.debug("📦 响应内容长度: " + (content != null ? content.length() : 0) + " 字符");
                    return content;
                } finally {
                    // 确保实体被消耗
                    EntityUtils.consumeQuietly(entity);
                }
            }
            
            return null;
            
        } catch (java.net.SocketTimeoutException e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.warn("⏰ HTTP请求超时: " + url + " (耗时: " + duration + "ms)");
            // 取消请求以释放连接
            request.abort();
            throw new IOException("Request timeout for URL: " + url, e);
        } catch (org.apache.http.conn.ConnectionPoolTimeoutException e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.warn("📦 连接池超时: " + url + " (耗时: " + duration + "ms)");
            throw new IOException("Connection pool timeout for URL: " + url, e);
        } catch (java.net.ConnectException e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.warn("🔌 连接失败: " + url + " (耗时: " + duration + "ms)");
            throw new IOException("Connection failed for URL: " + url, e);
        } catch (IOException e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.warn("❌ HTTP请求异常: " + url + " (耗时: " + duration + "ms) - " + e.getMessage());
            // 取消请求以释放连接
            request.abort();
            throw e;
        }
    }

    @Override
    public void close() throws IOException {
        try {
            if (httpClient != null) {
                httpClient.close();
            }
            if (connectionManager != null) {
                connectionManager.close();
            }
            logger.debug("🔌 HTTP客户端和连接池已关闭");
        } catch (Exception e) {
            logger.warn("⚠️ 关闭HTTP客户端时出错: " + e.getMessage());
        }
    }

    /**
     * 使用新的SEC搜索API获取13F文件列表
     */
    public List<Filing> getCompanyFilingsWithSearchAPI(String cik) throws IOException, InterruptedException {
        rateLimitRequest();
        
        // 构建搜索URL
        String searchUrl = buildSearchApiUrl(cik);
        logger.secRequest(searchUrl, 0);
        
        String jsonResponse = executeGetRequest(searchUrl);
        List<Filing> filings = new ArrayList<>();
        
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode hits = root.path("hits").path("hits");
            
            for (JsonNode hit : hits) {
                JsonNode source = hit.path("_source");
                String form = source.path("form").asText();
                String fileType = source.path("file_type").asText();
                
                // 只处理13F-HR文件中的INFORMATION TABLE类型
                if ("13F-HR".equals(form) && "INFORMATION TABLE".equals(fileType)) {
                    String _id = hit.path("_id").asText();
                    String adsh = source.path("adsh").asText();
                    String periodEnding = source.path("period_ending").asText();
                    String fileDate = source.path("file_date").asText();
                    String xsl = source.path("xsl").asText();
                    
                    // 从display_names字段解析CIK和公司名称
                    String recordCik = null;
                    String companyName = null;
                    JsonNode displayNames = source.path("display_names");
                    if (displayNames.isArray() && displayNames.size() > 0) {
                        String displayName = displayNames.get(0).asText();
                        logger.debug("📋 从display_names提取到: " + displayName);
                        
                        // display_names格式通常为: "COMPANY NAME (CIK 0001234567)"
                        if (displayName.contains("(CIK ") && displayName.endsWith(")")) {
                            int cikStart = displayName.lastIndexOf("(CIK ") + 5;
                            int cikEnd = displayName.lastIndexOf(")");
                            if (cikStart < cikEnd) {
                                recordCik = displayName.substring(cikStart, cikEnd).trim();
                                companyName = displayName.substring(0, displayName.lastIndexOf("(CIK ")).trim();
                                logger.debug("✅ 解析得到 CIK: " + recordCik + ", 公司名称: " + companyName);
                            }
                        } else {
                            // 如果格式不符合预期，使用整个display_names作为公司名称，CIK使用查询参数
                            companyName = displayName;
                            recordCik = cik;
                            logger.debug("⚠️ display_names格式异常，使用查询CIK: " + recordCik + ", 公司名称: " + companyName);
                        }
                    } else {
                        // 如果没有display_names，使用查询参数
                        recordCik = cik;
                        logger.debug("⚠️ 未找到display_names，使用查询CIK: " + recordCik);
                    }
                    
                    // 从_id中解析文件名: accessionNumber:fileName
                    String[] idParts = _id.split(":");
                    if (idParts.length == 2) {
                        String accessionNumber = idParts[0];
                        String fileName = idParts[1];
                        
                        Filing filing = new Filing();
                        filing.setCik(recordCik); // 使用从display_names解析的CIK
                        filing.setAccessionNumber(accessionNumber);
                        filing.setFilingType(form);
                        // 设置从display_names解析到的公司名称
                        if (companyName != null && !companyName.trim().isEmpty()) {
                            filing.setCompanyName(companyName);
                        }
                        
                        // 解析文件日期作为filing date
                        if (fileDate != null && !fileDate.isEmpty()) {
                            try {
                                filing.setFilingDate(LocalDate.parse(fileDate));
                            } catch (Exception e) {
                                logger.debug("解析filing date失败: " + fileDate);
                            }
                        }
                        
                        // 解析报告期间
                        if (periodEnding != null && !periodEnding.isEmpty()) {
                            try {
                                filing.setReportPeriod(LocalDate.parse(periodEnding));
                            } catch (Exception e) {
                                logger.debug("解析report period失败: " + periodEnding);
                            }
                        }
                        
                        // 设置form_file和其他字段用于后续URL构建
                        filing.setFormFile(fileName);
                        
                        // 获取持仓数据，传入从display_names解析的CIK和公司名称
                        List<Holding> holdings = getHoldingsFromSearchResult(recordCik, companyName, accessionNumber, fileName, xsl);
                        filing.setHoldings(holdings);
                        
                        filings.add(filing);
                    }
                }
            }
            
            logger.info("📊 通过搜索API找到 " + filings.size() + " 个13F文件");
            // 记录解析到的不同机构信息
            if (!filings.isEmpty()) {
                logger.info("📋 包含以下机构的持仓数据:");
                filings.stream()
                    .collect(java.util.stream.Collectors.groupingBy(f -> f.getCik() + ":" + f.getCompanyName()))
                    .forEach((key, groupFilings) -> {
                        String[] parts = key.split(":", 2);
                        String parsedCik = parts[0];
                        String parsedCompanyName = parts.length > 1 ? parts[1] : "未知";
                        logger.info("  🏢 CIK: " + parsedCik + ", 公司: " + parsedCompanyName + " (" + groupFilings.size() + " 个文件)");
                    });
            }
            return filings;
            
        } catch (Exception e) {
            logger.error("解析搜索API响应失败", e);
            throw new IOException("Failed to parse search API response: " + e.getMessage());
        }
    }

    /**
     * 根据搜索API结果获取持仓数据
     */
    private List<Holding> getHoldingsFromSearchResult(String cik, String companyName, String accessionNumber, String fileName, String xsl) throws IOException, InterruptedException {
        rateLimitRequest();
        
        // 构建完整的持仓文件URL
        String cikRemovePrefixZero = removeLeadingZeros(cik);
        String accessionNumberClean = accessionNumber.replaceAll("-", "");
        String holdingsUrl = SEC_ARCHIVES_BASE_URL + "/edgar/data/" + cikRemovePrefixZero + "/" + accessionNumberClean + "/" + xsl + "/" + fileName;
        
        logger.debug("📊 获取持仓数据: " + holdingsUrl);
        
        try {
            String xmlContent = executeGetRequest(holdingsUrl);
            if (xmlContent != null && !xmlContent.trim().isEmpty()) {
                // 使用智能格式检测和相应的解析器解析持仓数据
                Filing tempFiling = parse13FContent(xmlContent, accessionNumber, cik);
                List<Holding> holdings = (tempFiling != null) ? tempFiling.getHoldings() : new ArrayList<>();
                
                // 为每个holding设置CIK和公司名称
                if (holdings != null) {
                    for (Holding holding : holdings) {
                        holding.setCik(cik);
                        if (companyName != null && !companyName.trim().isEmpty()) {
                            holding.setCompanyName(companyName);
                        }
                    }
                }
                
                logger.info("✅ 成功解析 " + holdings.size() + " 条持仓记录");
                return holdings;
            }
        } catch (Exception e) {
            logger.warn("⚠️ 获取持仓数据失败: " + holdingsUrl + " - " + e.getMessage());
        }
        
        return new ArrayList<>();
    }

    /**
     * 构建搜索API的URL
     */
    private String buildSearchApiUrl(String cik) {
        // 构建搜索参数
        // 示例: https://efts.sec.gov/LATEST/search-index?q=13F&dateRange=custom&category=form-cat0&ciks=0001166559&entityName=GATES%20FOUNDATION%20TRUST%20(CIK%200001166559)&startdt=2020-09-01&enddt=2025-09-07&forms=-3%2C-4%2C-5
        
        String formattedCik = formatCik(cik);
        LocalDate startDate = LocalDate.now().minusYears(5); // 过去5年的数据
        LocalDate endDate = LocalDate.now();
        
        StringBuilder urlBuilder = new StringBuilder(SEC_SEARCH_API_URL);
        urlBuilder.append("?q=13F");
        urlBuilder.append("&dateRange=custom");
        urlBuilder.append("&category=form-cat0");
        urlBuilder.append("&ciks=").append(formattedCik);
        urlBuilder.append("&startdt=").append(startDate.toString());
        urlBuilder.append("&enddt=").append(endDate.toString());
        urlBuilder.append("&forms=-3%2C-4%2C-5");
        
        return urlBuilder.toString();
    }

    /**
     * 移除CIK前导零
     */
    private String removeLeadingZeros(String cik) {
        return String.valueOf(Long.parseLong(cik));
    }

    /**
     * 使用新的搜索API获取最新的13F文件
     */
    public Filing getLatest13FWithSearchAPI(String cik) throws IOException, InterruptedException {
        List<Filing> filings = getCompanyFilingsWithSearchAPI(cik);
        if (filings.isEmpty()) {
            throw new IOException("No 13F filings found for CIK: " + cik);
        }
        
        // 按文件日期排序，返回最新的
        filings.sort((f1, f2) -> {
            LocalDate d1 = f1.getFilingDate();
            LocalDate d2 = f2.getFilingDate();
            if (d1 == null && d2 == null) return 0;
            if (d1 == null) return 1;
            if (d2 == null) return -1;
            return d2.compareTo(d1);
        });
        
        return filings.get(0);
    }

    /**
     * 检测内容是否为HTML格式
     */
    private boolean isHTMLFormat(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        
        String trimmedContent = content.trim();
        String lowerContent = trimmedContent.toLowerCase();
        
        // 1. 检测HTML DOCTYPE声明
        boolean hasDoctype = lowerContent.startsWith("<!doctype html");
        
        // 2. 检测HTML标签
        boolean hasHtmlTag = lowerContent.contains("<html");
        
        // 3. 检测HTML body标签  
        boolean hasBodyTag = lowerContent.contains("<body>");
        
        // 4. 检测meta标签（这是导致XML解析失败的关键标识）
        boolean hasMetaTag = lowerContent.contains("<meta");
        
        // 5. 检测SEC特有的HTML表格样式类
        boolean hasFormDataClass = lowerContent.contains("class=\"formdata");
        
        // 6. 检测table标签
        boolean hasTableTag = lowerContent.contains("<table");
        
        // 7. 排除XML声明
        boolean hasXmlDeclaration = trimmedContent.startsWith("<?xml");
        
        logger.debug("🔍 格式检测结果:");
        logger.debug("  DOCTYPE: " + hasDoctype);
        logger.debug("  HTML标签: " + hasHtmlTag);
        logger.debug("  BODY标签: " + hasBodyTag);
        logger.debug("  META标签: " + hasMetaTag);
        logger.debug("  TABLE标签: " + hasTableTag);
        logger.debug("  FormData样式: " + hasFormDataClass);
        logger.debug("  XML声明: " + hasXmlDeclaration);
        
        // 如果有XML声明，优先判断为XML格式
        if (hasXmlDeclaration) {
            return false;
        }
        
        // 如果有DOCTYPE HTML或HTML标签，判断为HTML格式
        if (hasDoctype || hasHtmlTag) {
            return true;
        }
        
        // 如果有meta标签和body标签，很可能是HTML
        if (hasMetaTag && hasBodyTag) {
            return true;
        }
        
        // 如果有SEC特有的表格样式，判断为HTML格式
        if (hasTableTag && hasFormDataClass) {
            return true;
        }
        
        return false;
    }
}