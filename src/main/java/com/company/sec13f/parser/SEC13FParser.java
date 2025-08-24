package com.company.sec13f.parser;

import com.company.sec13f.parser.model.Filing;
import com.company.sec13f.parser.model.Holding;
import com.company.sec13f.parser.report.ReportGenerator;
import com.company.sec13f.parser.scraper.SECScraper;
import com.company.sec13f.parser.service.FilingService;

import java.io.IOException;
import java.util.List;

/**
 * Main application class for the SEC 13F parser
 */
public class SEC13FParser {

    public static void main(String[] args) {
        // This is a placeholder for the main application logic
        // A real implementation would parse command line arguments,
        // retrieve filings, compare them, and generate reports
        
        System.out.println("SEC 13F Parser");
        System.out.println("==============");
        System.out.println("This application is designed to scrape SEC 13F filings,");
        System.out.println("parse the data, and generate reports on investment holdings differences.");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  java SEC13FParser <CIK> <output_format> <output_file>");
        System.out.println();
        System.out.println("Parameters:");
        System.out.println("  CIK             The CIK number of the company to analyze");
        System.out.println("  output_format   The format of the output report (csv or excel)");
        System.out.println("  output_file     The name of the file to save the report to");
        System.out.println();
        System.out.println("Example:");
        System.out.println("  java SEC13FParser 0001166559 excel holdings_report.xlsx");
    }
}