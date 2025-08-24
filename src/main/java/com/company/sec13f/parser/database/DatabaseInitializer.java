package com.company.sec13f.parser.database;

import java.sql.SQLException;

/**
 * Initializes the database for the SEC 13F parser application
 */
public class DatabaseInitializer {
    
    /**
     * Initializes the database tables
     */
    public static void initializeDatabase() {
        FilingDAO dao = new FilingDAO();
        try {
            dao.initializeDatabase();
            System.out.println("Database initialized successfully.");
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        initializeDatabase();
    }
}