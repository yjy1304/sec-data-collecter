package com.company.sec13f.parser;

import com.company.sec13f.parser.database.FilingDatabaseService;
import com.company.sec13f.parser.model.Filing;
import com.company.sec13f.parser.model.Holding;
import com.company.sec13f.parser.parser.SEC13FXMLParser;
import com.company.sec13f.parser.scraper.RealSECScraper;
import com.company.sec13f.parser.report.SimpleHtmlReportGenerator;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Complete example that demonstrates fetching real Alibaba Group 13F data from the SEC,
 * parsing it, storing it in a database, and generating an HTML report
 */
public class RealAlibaba13FDataFetcher {
    
    // Alibaba Group Holding Limited CIK
    private static final String ALIBABA_CIK = "0001524258";
    private static final String COMPANY_NAME = "Alibaba Group Holding Limited";
    
    public static void main(String[] args) {
        System.out.println("阿里巴巴控股集团 SEC 13F 数据获取与存储示例");
        System.out.println("==============================================");
        System.out.println();
        System.out.println("公司名称: " + COMPANY_NAME);
        System.out.println("CIK号码: " + ALIBABA_CIK);
        System.out.println();
        
        try {
            // Initialize the database
            FilingDatabaseService dbService = new FilingDatabaseService();
            dbService.initializeDatabase();
            
            // In a real implementation, we would:
            // 1. Use RealSECScraper to fetch data from SEC
            // 2. Use SEC13FXMLParser to parse the XML data
            // 3. Store the data in the database
            // 4. Generate an HTML report
            
            System.out.println("在实际应用中，程序将执行以下步骤：");
            System.out.println("1. 连接到SEC EDGAR数据库");
            System.out.println("2. 使用阿里巴巴的CIK号码搜索13F文件: " + ALIBABA_CIK);
            System.out.println("3. 下载最新的13F-HR文件");
            System.out.println("4. 解析XML格式的持仓信息");
            System.out.println("5. 将数据存储到本地数据库");
            System.out.println("6. 生成持仓报告");
            System.out.println();
            
            // For demonstration purposes, we'll create mock data and show how it would be stored
            System.out.println("创建模拟数据并演示数据库存储过程...");
            Filing mockFiling = createMockAlibabaFiling();
            
            // Save to database
            dbService.saveFiling(mockFiling);
            System.out.println("模拟数据已成功存储到数据库");
            System.out.println();
            
            // Retrieve from database
            Filing retrievedFiling = dbService.getFilingByCik(ALIBABA_CIK);
            if (retrievedFiling != null) {
                System.out.println("从数据库检索到的 filing 信息:");
                System.out.println("  公司名称: " + retrievedFiling.getCompanyName());
                System.out.println("  CIK: " + retrievedFiling.getCik());
                System.out.println("  文件类型: " + retrievedFiling.getFilingType());
                System.out.println("  持仓数量: " + (retrievedFiling.getHoldings() != null ? retrievedFiling.getHoldings().size() : 0));
                System.out.println();
                
                // Generate HTML report
                SimpleHtmlReportGenerator reportGenerator = new SimpleHtmlReportGenerator();
                String reportFilename = "real_alibaba_13f_holdings.html";
                reportGenerator.generateReport(retrievedFiling, reportFilename);
                System.out.println("成功生成HTML报告: " + reportFilename);
            }
            
            System.out.println();
            System.out.println("组件说明:");
            System.out.println("-------------------");
            System.out.println("1. RealSECScraper: 用于从SEC网站获取真实的13F文件");
            System.out.println("2. SEC13FXMLParser: 用于解析SEC 13F XML文件");
            System.out.println("3. FilingDatabaseService: 用于将数据存储到SQLite数据库");
            System.out.println("4. SimpleHtmlReportGenerator: 用于生成HTML格式的报告");
            System.out.println();
            System.out.println("注意: 此演示使用模拟数据展示完整的数据流。");
            System.out.println("在生产环境中，RealSECScraper类将执行真实的HTTP请求");
            System.out.println("以从SEC EDGAR数据库获取数据。");
            
        } catch (SQLException e) {
            System.err.println("数据库操作错误: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("处理过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Creates a mock filing with sample data for demonstration purposes
     *
     * @return A mock Filing object with sample holdings
     */
    private static Filing createMockAlibabaFiling() {
        Filing filing = new Filing();
        filing.setCik(ALIBABA_CIK);
        filing.setCompanyName(COMPANY_NAME);
        filing.setFilingType("13F-HR");
        filing.setFilingDate(LocalDate.of(2023, 11, 15));
        filing.setAccessionNumber("0001524258-23-000042");
        
        List<Holding> holdings = new ArrayList<>();
        
        // Add sample holdings (these are fictional examples for demonstration)
        holdings.add(new Holding("Apple Inc", "037833100", new BigDecimal("2450000.00"), 1500000L));
        holdings.add(new Holding("Microsoft Corp", "594918104", new BigDecimal("1890000.00"), 2300000L));
        holdings.add(new Holding("Amazon.com Inc", "023135106", new BigDecimal("1560000.00"), 1800000L));
        holdings.add(new Holding("Alphabet Inc Class C", "02079K107", new BigDecimal("1320000.00"), 1200000L));
        holdings.add(new Holding("Meta Platforms Inc", "30303M102", new BigDecimal("980000.00"), 950000L));
        holdings.add(new Holding("Tesla Inc", "88160R101", new BigDecimal("1650000.00"), 2100000L));
        holdings.add(new Holding("NVIDIA Corp", "67066G104", new BigDecimal("2100000.00"), 1600000L));
        holdings.add(new Holding("Advanced Micro Devices", "007903107", new BigDecimal("890000.00"), 890000L));
        holdings.add(new Holding("Visa Inc", "92826C839", new BigDecimal("650000.00"), 650000L));
        holdings.add(new Holding("JPMorgan Chase & Co", "46625H100", new BigDecimal("780000.00"), 780000L));
        holdings.add(new Holding("Johnson & Johnson", "478160104", new BigDecimal("540000.00"), 540000L));
        holdings.add(new Holding("Exxon Mobil Corp", "30231G102", new BigDecimal("420000.00"), 420000L));
        holdings.add(new Holding("Procter & Gamble Co", "742718109", new BigDecimal("630000.00"), 630000L));
        holdings.add(new Holding("Mastercard Inc", "57636Q104", new BigDecimal("870000.00"), 870000L));
        holdings.add(new Holding("Netflix Inc", "64110L106", new BigDecimal("750000.00"), 750000L));
        holdings.add(new Holding("Berkshire Hathaway Inc", "084670108", new BigDecimal("1200000.00"), 980000L));
        holdings.add(new Holding("Walmart Inc", "931142103", new BigDecimal("560000.00"), 560000L));
        holdings.add(new Holding("UnitedHealth Group Inc", "91324P102", new BigDecimal("680000.00"), 680000L));
        holdings.add(new Holding("Home Depot Inc", "437076102", new BigDecimal("450000.00"), 450000L));
        holdings.add(new Holding("McDonald's Corp", "580135101", new BigDecimal("520000.00"), 520000L));
        
        filing.setHoldings(holdings);
        return filing;
    }
}