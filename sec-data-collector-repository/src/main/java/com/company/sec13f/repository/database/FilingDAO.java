package com.company.sec13f.repository.database;

import com.company.sec13f.repository.model.Filing;
import com.company.sec13f.repository.model.Holding;
import com.company.sec13f.repository.util.Logger;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for interacting with the SEC 13F filings database
 */
public class FilingDAO {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/sec13f?useSSL=false&serverTimezone=UTC&characterEncoding=utf8";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "123456";
    private static final Logger logger = Logger.getInstance();
    
    // SQL statements
    private static final String CREATE_FILING_TABLE = 
        "CREATE TABLE IF NOT EXISTS filings (" +
        "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
        "cik VARCHAR(50) NOT NULL, " +
        "company_name VARCHAR(500) NOT NULL, " +
        "filing_type VARCHAR(50) NOT NULL, " +
        "filing_date DATE NOT NULL, " +
        "accession_number VARCHAR(100) NOT NULL, " +
        "form_file VARCHAR(200) NOT NULL, " +
        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
        "UNIQUE KEY unique_filing (accession_number, form_file)) " +
        "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        
    private static final String CREATE_HOLDING_TABLE = 
        "CREATE TABLE IF NOT EXISTS holdings (" +
        "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
        "filing_id BIGINT NOT NULL, " +
        "name_of_issuer VARCHAR(500) NOT NULL, " +
        "cusip VARCHAR(20) NOT NULL, " +
        "value DECIMAL(15,2), " +
        "shares BIGINT, " +
        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
        "FOREIGN KEY (filing_id) REFERENCES filings (id) ON DELETE CASCADE) " +
        "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        
    private static final String INSERT_FILING = 
        "INSERT INTO filings (cik, company_name, filing_type, filing_date, accession_number, form_file) " +
        "VALUES (?, ?, ?, ?, ?, ?)";
        
    private static final String INSERT_HOLDING = 
        "INSERT INTO holdings (filing_id, name_of_issuer, cusip, value, shares) " +
        "VALUES (?, ?, ?, ?, ?)";
        
    private static final String SELECT_FILINGS_BY_CIK = 
        "SELECT * FROM filings WHERE cik = ? ORDER BY filing_date DESC, form_file";
        
    private static final String SELECT_FILING_ID_BY_ACCESSION = 
        "SELECT id FROM filings WHERE accession_number = ? AND form_file = ?";
        
    private static final String SELECT_HOLDINGS_BY_FILING_ID = 
        "SELECT * FROM holdings WHERE filing_id = ?";
        
    private static final String SELECT_ALL_HOLDINGS_BY_CIK = 
        "SELECT h.*, f.filing_date, f.accession_number, f.form_file FROM holdings h " +
        "JOIN filings f ON h.filing_id = f.id " +
        "WHERE f.cik = ? " +
        "ORDER BY f.filing_date DESC, f.form_file, h.value DESC";

    /**
     * Initializes the database tables
     *
     * @throws SQLException If there's an error creating the tables
     */
    public void initializeDatabase() throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement()) {
            
            // MySQL has foreign key constraints enabled by default
            
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
        logger.info("📄 Saving filing to database - CIK: " + filing.getCik() + ", Accession: " + filing.getAccessionNumber());
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
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
                    pstmt.setString(6, filing.getFormFile() != null ? filing.getFormFile() : "unknown");
                    
                    int affectedRows = pstmt.executeUpdate();
                    
                    if (affectedRows == 0) {
                        throw new SQLException("Creating filing failed, no rows affected.");
                    }
                    
                    try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            filingId = generatedKeys.getLong(1);
                            logger.info("✅ Filing record created with ID: " + filingId);
                        } else {
                            throw new SQLException("Creating filing failed, no ID obtained.");
                        }
                    }
                }
                
                // Insert holdings
                if (filing.getHoldings() != null && !filing.getHoldings().isEmpty()) {
                    logger.info("💼 Saving " + filing.getHoldings().size() + " holdings to database");
                    int savedHoldings = 0;
                    
                    try (PreparedStatement pstmt = conn.prepareStatement(INSERT_HOLDING)) {
                        for (Holding holding : filing.getHoldings()) {
                            pstmt.setLong(1, filingId);
                            pstmt.setString(2, holding.getNameOfIssuer());
                            pstmt.setString(3, holding.getCusip());
                            pstmt.setBigDecimal(4, holding.getValue());
                            pstmt.setLong(5, holding.getShares() != null ? holding.getShares() : 0);
                            
                            int result = pstmt.executeUpdate();
                            if (result > 0) {
                                savedHoldings++;
                            }
                        }
                    }
                    logger.info("✅ Successfully saved " + savedHoldings + " holdings to database");
                } else {
                    logger.warn("⚠️ No holdings found in filing to save");
                }
                
                conn.commit();
                logger.info("🎯 Transaction committed successfully - Filing ID: " + filingId);
                return filingId;
                
            } catch (SQLException e) {
                conn.rollback();
                logger.error("❌ Database transaction rolled back due to error: " + e.getMessage());
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
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
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
                    filing.setFormFile(rs.getString("form_file"));
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
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
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
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
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
                    "GROUP BY f.id ORDER BY f.filing_date DESC, f.form_file";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Filing filing = new Filing();
                    filing.setCik(rs.getString("cik"));
                    filing.setAccessionNumber(rs.getString("accession_number"));
                    filing.setCompanyName(rs.getString("company_name"));
                    filing.setFilingType(rs.getString("filing_type"));
                    filing.setFormFile(rs.getString("form_file"));
                    
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
     * Gets filing ID by accession number and form file
     *
     * @param accessionNumber The accession number to search for
     * @param formFile The form file name to search for
     * @return The filing ID
     * @throws SQLException If there's an error retrieving the filing ID
     */
    public long getFilingIdByAccessionNumber(String accessionNumber, String formFile) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(SELECT_FILING_ID_BY_ACCESSION)) {
            
            pstmt.setString(1, accessionNumber);
            pstmt.setString(2, formFile != null ? formFile : "unknown");
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                } else {
                    throw new SQLException("Filing not found with accession number: " + accessionNumber + " and form file: " + formFile);
                }
            }
        }
    }
    
    /**
     * Gets filing ID by accession number (returns first match)
     *
     * @param accessionNumber The accession number to search for
     * @return The filing ID
     * @throws SQLException If there's an error retrieving the filing ID
     */
    public long getFilingIdByAccessionNumber(String accessionNumber) throws SQLException {
        String sql = "SELECT id FROM filings WHERE accession_number = ? LIMIT 1";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
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