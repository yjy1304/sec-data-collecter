package com.company.sec13f.parser;

import com.company.sec13f.parser.database.DatabaseInitializer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 * Main class to start the web server for the SEC 13F parser application
 */
public class WebServer {
    private static final int PORT = 8080;
    
    public static void main(String[] args) throws Exception {
        // Initialize the database
        DatabaseInitializer.initializeDatabase();
        
        // Create the server
        Server server = new Server(PORT);
        
        // Create the servlet context handler
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
        
        // Add servlets
        context.addServlet(new ServletHolder(new com.company.sec13f.parser.web.FilingsServlet()), "/filings/*");
        context.addServlet(new ServletHolder(new com.company.sec13f.parser.web.SearchServlet()), "/search");
        context.addServlet(new ServletHolder(new com.company.sec13f.parser.web.AnalysisServlet()), "/api/analysis/*");
        context.addServlet(new ServletHolder(new com.company.sec13f.parser.web.ScrapingServlet()), "/api/scraping/*");
        context.addServlet(new ServletHolder(new com.company.sec13f.parser.web.TaskManagementServlet()), "/tasks/*");
        ServletHolder homeServletHolder = new ServletHolder(new com.company.sec13f.parser.web.HomeServlet());
        homeServletHolder.setInitOrder(0);
        context.addServlet(homeServletHolder, "/");
        
        // Add static resources
        context.setResourceBase("src/main/resources/webapp");
        context.setWelcomeFiles(new String[]{ "index.html" });
        
        // Start the server
        server.start();
        System.out.println("Server started on port " + PORT);
        System.out.println("Visit http://localhost:" + PORT + " to access the application");
        server.join();
    }
}