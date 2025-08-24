package com.company.sec13f.parser;

import com.company.sec13f.parser.model.Filing;
import com.company.sec13f.parser.model.Holding;
import com.company.sec13f.parser.report.SimpleHtmlReportGenerator;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates how to generate an HTML report for Alibaba Group's 13F filing
 */
public class Alibaba13FReportDemo {
    
    // Alibaba Group Holding Limited CIK
    private static final String ALIBABA_CIK = "0001524258";
    private static final String COMPANY_NAME = "Alibaba Group Holding Limited";
    
    public static void main(String[] args) {
        System.out.println("Generating Alibaba Group 13F Holdings Report...");
        System.out.println("==================================================");
        
        try {
            // Create a mock filing with realistic sample data
            Filing mockFiling = createMockAlibabaFiling();
            
            // Generate HTML report
            SimpleHtmlReportGenerator reportGenerator = new SimpleHtmlReportGenerator();
            String reportFilename = "reports/alibaba_13f_holdings.html";
            reportGenerator.generateReport(mockFiling, reportFilename);
            
            System.out.println("Successfully generated HTML report: " + reportFilename);
            System.out.println();
            System.out.println("In a production environment, this program would:");
            System.out.println("1. Connect to the SEC EDGAR database");
            System.out.println("2. Search for filings using Alibaba's CIK: " + ALIBABA_CIK);
            System.out.println("3. Download the latest 13F-HR filing");
            System.out.println("4. Parse the XML data to extract holdings");
            System.out.println("5. Generate a comprehensive HTML report like the one created");
            
        } catch (IOException e) {
            System.err.println("Error generating report: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Creates a mock filing with realistic sample data for demonstration purposes
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
        
        // Add realistic sample holdings based on typical 13F filings
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