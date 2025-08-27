package com.company.sec13f.repository.database;

import com.company.sec13f.repository.model.Filing;
import com.company.sec13f.repository.model.Holding;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service class for database operations related to SEC 13F filings
 */
public class FilingDatabaseService {
    private static final String DB_URL = "jdbc:sqlite:sec13f.db";
    
    /**
     * Saves a filing and its holdings to the database
     *
     * @param filing The filing to save
     * @throws SQLException If an error occurs during the database operation
     */
    public void saveFiling(Filing filing) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.setAutoCommit(false);
            
            try {
                // Save the filing
                long filingId = saveFilingRecord(conn, filing);
                
                // Save the holdings
                saveHoldings(conn, filingId, filing.getHoldings());
                
                conn.commit();
                System.out.println("Successfully saved filing with ID: " + filingId);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }
    
    /**
     * Saves a filing record to the database
     *
     * @param conn The database connection
     * @param filing The filing to save
     * @return The ID of the saved filing record
     * @throws SQLException If an error occurs during the database operation
     */
    private long saveFilingRecord(Connection conn, Filing filing) throws SQLException {
        String sql = "INSERT INTO filings (cik, company_name, filing_type, filing_date, accession_number) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, filing.getCik());
            pstmt.setString(2, filing.getCompanyName());
            pstmt.setString(3, filing.getFilingType());
            pstmt.setDate(4, filing.getFilingDate() != null ? Date.valueOf(filing.getFilingDate()) : null);
            pstmt.setString(5, filing.getAccessionNumber());
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Creating filing failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                } else {
                    throw new SQLException("Creating filing failed, no ID obtained.");
                }
            }
        }
    }
    
    /**
     * Saves the holdings for a filing to the database
     *
     * @param conn The database connection
     * @param filingId The ID of the filing
     * @param holdings The list of holdings to save
     * @throws SQLException If an error occurs during the database operation
     */
    private void saveHoldings(Connection conn, long filingId, List<Holding> holdings) throws SQLException {
        if (holdings == null || holdings.isEmpty()) {
            return;
        }
        
        String sql = "INSERT INTO holdings (filing_id, name_of_issuer, cusip, value, shares) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (Holding holding : holdings) {
                pstmt.setLong(1, filingId);
                pstmt.setString(2, holding.getNameOfIssuer());
                pstmt.setString(3, holding.getCusip());
                pstmt.setBigDecimal(4, holding.getValue());
                pstmt.setLong(5, holding.getShares());
                
                pstmt.addBatch();
            }
            
            pstmt.executeBatch();
        }
    }
    
    /**
     * Retrieves a filing and its holdings from the database
     *
     * @param cik The CIK number of the company
     * @return The filing, or null if not found
     * @throws SQLException If an error occurs during the database operation
     */
    public Filing getFilingByCik(String cik) throws SQLException {
        String filingSql = "SELECT * FROM filings WHERE cik = ? ORDER BY filing_date DESC LIMIT 1";
        String holdingsSql = "SELECT * FROM holdings WHERE filing_id = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement filingStmt = conn.prepareStatement(filingSql)) {
            
            filingStmt.setString(1, cik);
            
            try (ResultSet filingRs = filingStmt.executeQuery()) {
                if (filingRs.next()) {
                    Filing filing = new Filing();
                    filing.setCik(filingRs.getString("cik"));
                    filing.setCompanyName(filingRs.getString("company_name"));
                    filing.setFilingType(filingRs.getString("filing_type"));
                    
                    Date filingDate = filingRs.getDate("filing_date");
                    if (filingDate != null) {
                        filing.setFilingDate(filingDate.toLocalDate());
                    }
                    
                    filing.setAccessionNumber(filingRs.getString("accession_number"));
                    
                    // Retrieve holdings
                    long filingId = filingRs.getLong("id");
                    try (PreparedStatement holdingsStmt = conn.prepareStatement(holdingsSql)) {
                        holdingsStmt.setLong(1, filingId);
                        
                        try (ResultSet holdingsRs = holdingsStmt.executeQuery()) {
                            while (holdingsRs.next()) {
                                Holding holding = new Holding();
                                holding.setNameOfIssuer(holdingsRs.getString("name_of_issuer"));
                                holding.setCusip(holdingsRs.getString("cusip"));
                                holding.setValue(holdingsRs.getBigDecimal("value"));
                                holding.setShares(holdingsRs.getLong("shares"));
                                
                                if (filing.getHoldings() == null) {
                                    filing.setHoldings(new ArrayList<>());
                                }
                                filing.getHoldings().add(holding);
                            }
                        }
                    }
                    
                    return filing;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Initializes the database tables if they don't exist
     *
     * @throws SQLException If an error occurs during the database operation
     */
    public void initializeDatabase() throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            
            // Enable foreign key constraints
            stmt.execute("PRAGMA foreign_keys = ON");
            
            // Create filings table
            stmt.execute("CREATE TABLE IF NOT EXISTS filings (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "cik TEXT NOT NULL, " +
                    "company_name TEXT NOT NULL, " +
                    "filing_type TEXT NOT NULL, " +
                    "filing_date DATE, " +
                    "accession_number TEXT NOT NULL UNIQUE)");
            
            // Create holdings table
            stmt.execute("CREATE TABLE IF NOT EXISTS holdings (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "filing_id INTEGER NOT NULL, " +
                    "name_of_issuer TEXT NOT NULL, " +
                    "cusip TEXT NOT NULL, " +
                    "value DECIMAL(15,2), " +
                    "shares BIGINT, " +
                    "FOREIGN KEY (filing_id) REFERENCES filings (id) ON DELETE CASCADE)");
            
            System.out.println("Database initialized successfully.");
        }
    }
}