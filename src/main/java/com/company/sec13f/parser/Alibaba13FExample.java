package com.company.sec13f.parser;

import com.company.sec13f.parser.model.Filing;
import com.company.sec13f.parser.model.Holding;
import com.company.sec13f.parser.report.SimpleHtmlReportGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Main class to demonstrate fetching and displaying Alibaba Group's SEC 13F filings
 */
public class Alibaba13FExample {
    
    // Alibaba Group Holding Limited CIK
    private static final String ALIBABA_CIK = "0001524258";
    private static final String COMPANY_NAME = "Alibaba Group Holding Limited";
    
    public static void main(String[] args) {
        System.out.println("阿里巴巴控股集团 SEC 13F 持仓分析");
        System.out.println("====================================");
        System.out.println();
        System.out.println("公司名称: " + COMPANY_NAME);
        System.out.println("CIK号码: " + ALIBABA_CIK);
        System.out.println();
        
        try {
            // 创建一个模拟的13F文件（实际应用中会从SEC网站获取）
            Filing mockFiling = createMockAlibabaFiling();
            
            // 生成HTML报告
            SimpleHtmlReportGenerator reportGenerator = new SimpleHtmlReportGenerator();
            String reportFilename = "alibaba_13f_holdings.html";
            reportGenerator.generateReport(mockFiling, reportFilename);
            
            System.out.println("成功生成HTML报告: " + reportFilename);
            System.out.println();
            System.out.println("部分持仓信息预览:");
            System.out.println("-------------------");
            
            List<Holding> holdings = mockFiling.getHoldings();
            if (holdings != null && !holdings.isEmpty()) {
                System.out.printf("%-25s %-12s %-15s %-15s%n", 
                    "发行人名称", "CUSIP", "股数", "价值($1000)");
                System.out.println("------------------------------------------------------------");
                
                // 显示前10个持仓
                for (int i = 0; i < Math.min(10, holdings.size()); i++) {
                    Holding holding = holdings.get(i);
                    System.out.printf("%-25s %-12s %-15s $%-14.2f%n",
                        truncateString(holding.getNameOfIssuer(), 25),
                        holding.getCusip(),
                        formatNumber(holding.getShares()),
                        holding.getValue() != null ? holding.getValue().doubleValue() : 0.0);
                }
                
                if (holdings.size() > 10) {
                    System.out.println("... 还有 " + (holdings.size() - 10) + " 个持仓");
                }
            }
            
            System.out.println();
            System.out.println("注意: 这是一个演示程序，使用模拟数据展示功能。");
            System.out.println("在实际应用中，程序将:");
            System.out.println("1. 连接到SEC EDGAR数据库");
            System.out.println("2. 使用阿里巴巴的CIK号码搜索13F文件: " + ALIBABA_CIK);
            System.out.println("3. 下载最新的13F-HR文件");
            System.out.println("4. 解析XML格式的持仓信息");
            System.out.println("5. 生成包含完整持仓信息的HTML报告");
            
        } catch (Exception e) {
            System.err.println("生成报告时出错: " + e.getMessage());
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
        
        // 添加一些模拟的持仓数据（这些是虚构的示例数据）
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
    
    /**
     * Truncates a string to a maximum length and adds ellipsis if needed
     *
     * @param str The string to truncate
     * @param maxLength The maximum length
     * @return The truncated string
     */
    private static String truncateString(String str, int maxLength) {
        if (str == null) {
            return "";
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }
    
    /**
     * Formats a number with commas
     *
     * @param number The number to format
     * @return The formatted number as a string
     */
    private static String formatNumber(Long number) {
        if (number == null) {
            return "N/A";
        }
        return String.format("%,d", number);
    }
}