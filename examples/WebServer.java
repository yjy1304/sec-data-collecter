package com.company.sec13f.parser;

import com.company.sec13f.parser.config.ApplicationConfig;
import com.company.sec13f.parser.database.DatabaseInitializer;
import com.company.sec13f.parser.service.ScheduledScrapingService;
import com.company.sec13f.parser.web.SchedulingServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Main class to start the web server for the SEC 13F parser application
 */
public class WebServer {
    private static final int PORT = 8080;
    
    public static void main(String[] args) throws Exception {
        // Initialize the database
        DatabaseInitializer.initializeDatabase();
        
        // Initialize Spring Application Context
        AnnotationConfigApplicationContext applicationContext = 
            new AnnotationConfigApplicationContext(ApplicationConfig.class);
        
        System.out.println("Spring Application Context initialized successfully");
        
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
        
        // Add scheduling servlet with Spring integration
        SchedulingServlet schedulingServlet = new SchedulingServlet();
        try {
            ScheduledScrapingService scheduledScrapingService = applicationContext.getBean(ScheduledScrapingService.class);
            schedulingServlet.setScheduledScrapingService(scheduledScrapingService);
            System.out.println("Successfully configured SchedulingServlet with Spring bean");
        } catch (Exception e) {
            System.err.println("Failed to get ScheduledScrapingService bean: " + e.getMessage());
            e.printStackTrace();
        }
        context.addServlet(new ServletHolder(schedulingServlet), "/api/scheduling/*");
        
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
        
        // Add shutdown hook to properly close Spring context
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down Spring Application Context...");
            applicationContext.close();
        }));
        
        server.join();
    }
}