package com.company.sec13f.parser;

import com.company.sec13f.parser.database.FilingDAO;
import com.company.sec13f.parser.model.Filing;
import com.company.sec13f.parser.model.Holding;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Script to populate the database with sample Alibaba filing data
 */
public class PopulateAlibabaData {
    
    // Alibaba Group Holding Limited CIK
    private static final String ALIBABA_CIK = "0001524258";
    private static final String COMPANY_NAME = "Alibaba Group Holding Limited";
    
    public static void main(String[] args) {
        System.out.println("Populating database with sample Alibaba 13F filing data...");
        System.out.println("========================================================");
        
        try {
            // Initialize the database
            FilingDAO dao = new FilingDAO();
            dao.initializeDatabase();
            
            // Create sample filings
            Filing filing = createSampleFiling();
            
            // Save to database
            long filingId = dao.saveFiling(filing);
            System.out.println("Successfully saved filing with ID: " + filingId);
            
            System.out.println("Database populated successfully!");
            System.out.println("\nTo view the data in a web browser:");
            System.out.println("1. Run: mvn exec:java -Dexec.mainClass=\"com.company.sec13f.parser.WebServer\"");
            System.out.println("2. Open your web browser to: http://localhost:8080");
            
        } catch (SQLException e) {
            System.err.println("Error populating database: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Creates a sample filing with realistic data
     *
     * @return A sample Filing object
     */
    private static Filing createSampleFiling() {
        Filing filing = new Filing();
        filing.setCik(ALIBABA_CIK);
        filing.setCompanyName(COMPANY_NAME);
        filing.setFilingType("13F-HR");
        filing.setFilingDate(LocalDate.of(2023, 11, 15));
        filing.setAccessionNumber("0001524258-23-000042");
        
        List<Holding> holdings = new ArrayList<>();
        
        // Add realistic sample holdings
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