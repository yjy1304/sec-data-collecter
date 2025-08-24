package com.company.sec13f.parser.database;

import com.company.sec13f.parser.model.Filing;
import com.company.sec13f.parser.model.Holding;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for interacting with the SEC 13F filings database
 */
public class FilingDAO {
    private static final String DB_URL = "jdbc:sqlite:filings.db";
    
    // SQL statements
    private static final String CREATE_FILING_TABLE = 
        "CREATE TABLE IF NOT EXISTS filings (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "cik TEXT NOT NULL, " +
        "company_name TEXT NOT NULL, " +
        "filing_type TEXT NOT NULL, " +
        "filing_date DATE NOT NULL, " +
        "accession_number TEXT NOT NULL UNIQUE)";
        
    private static final String CREATE_HOLDING_TABLE = 
        "CREATE TABLE IF NOT EXISTS holdings (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "filing_id INTEGER NOT NULL, " +
        "name_of_issuer TEXT NOT NULL, " +
        "cusip TEXT NOT NULL, " +
        "value DECIMAL(15,2), " +
        "shares BIGINT, " +
        "FOREIGN KEY (filing_id) REFERENCES filings (id) ON DELETE CASCADE)";
        
    private static final String INSERT_FILING = 
        "INSERT INTO filings (cik, company_name, filing_type, filing_date, accession_number) " +
        "VALUES (?, ?, ?, ?, ?)";
        
    private static final String INSERT_HOLDING = 
        "INSERT INTO holdings (filing_id, name_of_issuer, cusip, value, shares) " +
        "VALUES (?, ?, ?, ?, ?)";
        
    private static final String SELECT_FILINGS_BY_CIK = 
        "SELECT * FROM filings WHERE cik = ? ORDER BY filing_date DESC";
        
    private static final String SELECT_FILING_ID_BY_ACCESSION = 
        "SELECT id FROM filings WHERE accession_number = ?";
        
    private static final String SELECT_HOLDINGS_BY_FILING_ID = 
        "SELECT * FROM holdings WHERE filing_id = ?";
        
    private static final String SELECT_ALL_HOLDINGS_BY_CIK = 
        "SELECT h.*, f.filing_date, f.accession_number FROM holdings h " +
        "JOIN filings f ON h.filing_id = f.id " +
        "WHERE f.cik = ? " +
        "ORDER BY f.filing_date DESC, h.value DESC";

    /**
     * Initializes the database tables
     *
     * @throws SQLException If there's an error creating the tables
     */
    public void initializeDatabase() throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            
            // Enable foreign key constraints
            stmt.execute("PRAGMA foreign_keys = ON");
            
            // Create tables
            stmt.execute(CREATE_FILING_TABLE);
            stmt.execute(CREATE_HOLDING_TABLE);
        }
    }

    /**
     * Saves a filing and its holdings to the database
     *
     * @param filing The filing to save
     * @return The ID of the saved filing
     * @throws SQLException If there's an error saving the filing
     */
    public long saveFiling(Filing filing) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.setAutoCommit(false);
            
            try {
                long filingId;
                
                // Insert filing
                try (PreparedStatement pstmt = conn.prepareStatement(INSERT_FILING, Statement.RETURN_GENERATED_KEYS)) {
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
                            filingId = generatedKeys.getLong(1);
                        } else {
                            throw new SQLException("Creating filing failed, no ID obtained.");
                        }
                    }
                }
                
                // Insert holdings
                if (filing.getHoldings() != null) {
                    try (PreparedStatement pstmt = conn.prepareStatement(INSERT_HOLDING)) {
                        for (Holding holding : filing.getHoldings()) {
                            pstmt.setLong(1, filingId);
                            pstmt.setString(2, holding.getNameOfIssuer());
                            pstmt.setString(3, holding.getCusip());
                            pstmt.setBigDecimal(4, holding.getValue());
                            pstmt.setLong(5, holding.getShares());
                            
                            pstmt.executeUpdate();
                        }
                    }
                }
                
                conn.commit();
                return filingId;
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Retrieves all filings for a given CIK
     *
     * @param cik The CIK to search for
     * @return A list of filings
     * @throws SQLException If there's an error retrieving the filings
     */
    public List<Filing> getFilingsByCik(String cik) throws SQLException {
        List<Filing> filings = new ArrayList<>();
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(SELECT_FILINGS_BY_CIK)) {
            
            pstmt.setString(1, cik);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Filing filing = new Filing();
                    filing.setCik(rs.getString("cik"));
                    filing.setCompanyName(rs.getString("company_name"));
                    filing.setFilingType(rs.getString("filing_type"));
                    
                    Date filingDate = rs.getDate("filing_date");
                    if (filingDate != null) {
                        filing.setFilingDate(filingDate.toLocalDate());
                    }
                    
                    filing.setAccessionNumber(rs.getString("accession_number"));
                    filings.add(filing);
                }
            }
        }
        
        return filings;
    }

    /**
     * Retrieves all holdings for a given filing ID
     *
     * @param filingId The filing ID to search for
     * @return A list of holdings
     * @throws SQLException If there's an error retrieving the holdings
     */
    public List<Holding> getHoldingsByFilingId(long filingId) throws SQLException {
        List<Holding> holdings = new ArrayList<>();
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(SELECT_HOLDINGS_BY_FILING_ID)) {
            
            pstmt.setLong(1, filingId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Holding holding = new Holding();
                    holding.setNameOfIssuer(rs.getString("name_of_issuer"));
                    holding.setCusip(rs.getString("cusip"));
                    holding.setValue(rs.getBigDecimal("value"));
                    holding.setShares(rs.getLong("shares"));
                    holdings.add(holding);
                }
            }
        }
        
        return holdings;
    }

    /**
     * Retrieves all holdings for a given CIK, ordered by filing date and value
     *
     * @param cik The CIK to search for
     * @return A list of holdings with filing information
     * @throws SQLException If there's an error retrieving the holdings
     */
    public List<HoldingWithFilingInfo> getAllHoldingsByCik(String cik) throws SQLException {
        List<HoldingWithFilingInfo> holdings = new ArrayList<>();
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(SELECT_ALL_HOLDINGS_BY_CIK)) {
            
            pstmt.setString(1, cik);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    HoldingWithFilingInfo holdingWithInfo = new HoldingWithFilingInfo();
                    
                    // Set holding information
                    Holding holding = new Holding();
                    holding.setNameOfIssuer(rs.getString("name_of_issuer"));
                    holding.setCusip(rs.getString("cusip"));
                    holding.setValue(rs.getBigDecimal("value"));
                    holding.setShares(rs.getLong("shares"));
                    holdingWithInfo.setHolding(holding);
                    
                    // Set filing information
                    holdingWithInfo.setFilingDate(rs.getDate("filing_date").toLocalDate());
                    holdingWithInfo.setAccessionNumber(rs.getString("accession_number"));
                    
                    holdings.add(holdingWithInfo);
                }
            }
        }
        
        return holdings;
    }
    
    /**
     * Retrieves all filings with basic information and holdings count
     *
     * @return A list of all filings
     * @throws SQLException If there's an error retrieving the filings
     */
    public List<Filing> getAllFilings() throws SQLException {
        List<Filing> filings = new ArrayList<>();
        
        String sql = "SELECT f.*, COUNT(h.id) as holdings_count FROM filings f " +
                    "LEFT JOIN holdings h ON f.id = h.filing_id " +
                    "GROUP BY f.id ORDER BY f.filing_date DESC";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Filing filing = new Filing();
                    filing.setCik(rs.getString("cik"));
                    filing.setAccessionNumber(rs.getString("accession_number"));
                    filing.setCompanyName(rs.getString("company_name"));
                    filing.setFilingType(rs.getString("filing_type"));
                    
                    Date filingDate = rs.getDate("filing_date");
                    if (filingDate != null) {
                        filing.setFilingDate(filingDate.toLocalDate());
                    }
                    
                    // 设置持仓数量（用于显示）
                    int holdingsCount = rs.getInt("holdings_count");
                    // 这里我们暂时创建一个空的holdings列表来存储数量信息
                    // 在实际使用中，可以考虑在Filing类中添加一个holdingsCount字段
                    
                    filings.add(filing);
                }
            }
        }
        
        return filings;
    }

    /**
     * Gets filing ID by accession number
     *
     * @param accessionNumber The accession number to search for
     * @return The filing ID
     * @throws SQLException If there's an error retrieving the filing ID
     */
    public long getFilingIdByAccessionNumber(String accessionNumber) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(SELECT_FILING_ID_BY_ACCESSION)) {
            
            pstmt.setString(1, accessionNumber);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                } else {
                    throw new SQLException("Filing not found with accession number: " + accessionNumber);
                }
            }
        }
    }
    
    /**
     * Helper class to hold holding information along with filing details
     */
    public static class HoldingWithFilingInfo {
        private Holding holding;
        private LocalDate filingDate;
        private String accessionNumber;
        
        public Holding getHolding() {
            return holding;
        }
        
        public void setHolding(Holding holding) {
            this.holding = holding;
        }
        
        public LocalDate getFilingDate() {
            return filingDate;
        }
        
        public void setFilingDate(LocalDate filingDate) {
            this.filingDate = filingDate;
        }
        
        public String getAccessionNumber() {
            return accessionNumber;
        }
        
        public void setAccessionNumber(String accessionNumber) {
            this.accessionNumber = accessionNumber;
        }
    }
}