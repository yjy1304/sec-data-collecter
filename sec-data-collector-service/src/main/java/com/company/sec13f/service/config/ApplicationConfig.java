package com.company.sec13f.service.config;

import com.company.sec13f.repository.database.FilingDAO;
import com.company.sec13f.repository.database.TaskDAO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Application Configuration - Only for non-annotated classes
 * Services with @Service annotation will be auto-discovered
 */
@Configuration
@Import(SchedulingConfig.class)
public class ApplicationConfig {

    @Bean
    public FilingDAO filingDAO() {
        return new FilingDAO();
    }

    @Bean
    public TaskDAO taskDAO() {
        return new TaskDAO();
    }
}