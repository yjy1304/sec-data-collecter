package com.company.sec13f.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Main Spring Boot application class for SEC Data Collector
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.company.sec13f"
})
public class SecDataCollectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecDataCollectorApplication.class, args);
    }
}