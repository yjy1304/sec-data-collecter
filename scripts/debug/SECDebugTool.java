package com.company.sec13f.parser.debug;

import com.company.sec13f.parser.scraper.RealSECScraper;
import com.company.sec13f.parser.model.Filing;
import com.company.sec13f.parser.database.FilingDAO;

public class SECDebugTool {
    
    public static void main(String[] args) {
        try {
            RealSECScraper scraper = new RealSECScraper();
            FilingDAO filingDAO = new FilingDAO();
            
            // 初始化数据库
            filingDAO.initializeDatabase();
            System.out.println("Database initialized successfully");
            
            System.out.println("Testing CIK: 0001067983 (Berkshire Hathaway)");
            
            // 获取文件列表
            java.util.List<Filing> filings = scraper.getCompanyFilings("0001067983");
            System.out.println("Found " + filings.size() + " 13F filings");
            
            if (!filings.isEmpty()) {
                Filing latest = filings.get(0);
                System.out.println("Latest filing: " + latest.getAccessionNumber());
                System.out.println("Filing date: " + latest.getFilingDate());
                
                // 获取详细信息
                Filing details = scraper.get13FDetails(latest.getAccessionNumber(), "0001067983");
                System.out.println("Holdings count: " + (details.getHoldings() != null ? details.getHoldings().size() : 0));
                
                if (details.getHoldings() != null && !details.getHoldings().isEmpty()) {
                    System.out.println("\nFirst few holdings:");
                    int count = Math.min(3, details.getHoldings().size());
                    for (int i = 0; i < count; i++) {
                        com.company.sec13f.parser.model.Holding holding = details.getHoldings().get(i);
                        System.out.println("- " + holding.getNameOfIssuer() + " | " + holding.getCusip() + " | " + holding.getValue());
                    }
                    
                    // 保存到数据库
                    details.setCompanyName("BERKSHIRE HATHAWAY INC");
                    long filingId = filingDAO.saveFiling(details);
                    System.out.println("\n✅ Filing saved to database with ID: " + filingId);
                    
                    // 验证数据是否保存成功
                    java.util.List<Filing> savedFilings = filingDAO.getFilingsByCik("0001067983");
                    System.out.println("✅ Database verification: Found " + savedFilings.size() + " saved filings for CIK 0001067983");
                }
            }
            
            scraper.close();
            
        } catch (Exception e) {
            System.err.println("❌ Error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
}