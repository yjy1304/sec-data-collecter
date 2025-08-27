package com.company.sec13f.parser.report;

import com.company.sec13f.parser.model.Filing;
import com.company.sec13f.parser.model.Holding;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Simple HTML report generator for 13F filings
 */
public class SimpleHtmlReportGenerator {
    
    /**
     * Generates an HTML report for a 13F filing
     *
     * @param filing The filing to generate a report for
     * @param filename The output filename
     * @throws IOException If there's an error writing the file
     */
    public void generateReport(Filing filing, String filename) throws IOException {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write(createHtmlReport(filing));
        }
    }
    
    /**
     * Creates an HTML report for a 13F filing
     *
     * @param filing The filing to create a report for
     * @return The HTML report as a string
     */
    private String createHtmlReport(Filing filing) {
        StringBuilder html = new StringBuilder();
        
        // HTML document structure
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n");
        html.append("<head>\n");
        html.append("    <title>13F Filing Report - ").append(escapeHtml(filing.getCompanyName())).append("</title>\n");
        html.append("    <style>\n");
        html.append("        body { font-family: Arial, sans-serif; margin: 20px; }\n");
        html.append("        h1 { color: #333; }\n");
        html.append("        h2 { color: #666; border-bottom: 1px solid #ccc; }\n");
        html.append("        table { border-collapse: collapse; width: 100%; margin: 20px 0; }\n");
        html.append("        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n");
        html.append("        th { background-color: #f2f2f2; }\n");
        html.append("        tr:nth-child(even) { background-color: #f9f9f9; }\n");
        html.append("        .info { background-color: #e7f3ff; padding: 15px; border-radius: 5px; margin: 20px 0; }\n");
        html.append("        .total { font-weight: bold; background-color: #dff0d8; }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        
        // Header
        html.append("    <h1>SEC Form 13F Holdings Report</h1>\n");
        
        // Filing information
        html.append("    <div class=\"info\">\n");
        html.append("        <h2>Filing Information</h2>\n");
        html.append("        <p><strong>Company Name:</strong> ").append(escapeHtml(filing.getCompanyName())).append("</p>\n");
        html.append("        <p><strong>CIK:</strong> ").append(escapeHtml(filing.getCik())).append("</p>\n");
        html.append("        <p><strong>Filing Type:</strong> ").append(escapeHtml(filing.getFilingType())).append("</p>\n");
        html.append("        <p><strong>Filing Date:</strong> ")
            .append(filing.getFilingDate() != null ? filing.getFilingDate().format(DateTimeFormatter.ofPattern("MM/dd/yyyy")) : "N/A")
            .append("</p>\n");
        html.append("        <p><strong>Accession Number:</strong> ").append(escapeHtml(filing.getAccessionNumber())).append("</p>\n");
        html.append("    </div>\n");
        
        // Holdings table
        html.append("    <h2>Holdings Details</h2>\n");
        html.append("    <table>\n");
        html.append("        <thead>\n");
        html.append("            <tr>\n");
        html.append("                <th>Name of Issuer</th>\n");
        html.append("                <th>CUSIP</th>\n");
        html.append("                <th>Shares</th>\n");
        html.append("                <th>Value (x $1000)</th>\n");
        html.append("                <th>Type</th>\n");
        html.append("            </tr>\n");
        html.append("        </thead>\n");
        html.append("        <tbody>\n");
        
        // Add holdings data
        List<Holding> holdings = filing.getHoldings();
        BigDecimal totalValue = BigDecimal.ZERO;
        
        if (holdings != null && !holdings.isEmpty()) {
            for (Holding holding : holdings) {
                html.append("            <tr>\n");
                html.append("                <td>").append(escapeHtml(holding.getNameOfIssuer())).append("</td>\n");
                html.append("                <td>").append(escapeHtml(holding.getCusip())).append("</td>\n");
                html.append("                <td>").append(holding.getShares() != null ? String.format("%,d", holding.getShares()) : "N/A").append("</td>\n");
                html.append("                <td>$").append(holding.getValue() != null ? String.format("%,.2f", holding.getValue()) : "0.00").append("</td>\n");
                html.append("                <td>SH</td>\n");
                html.append("            </tr>\n");
                
                if (holding.getValue() != null) {
                    totalValue = totalValue.add(holding.getValue());
                }
            }
            
            // Add total row
            html.append("            <tr class=\"total\">\n");
            html.append("                <td colspan=\"3\">Total Value</td>\n");
            html.append("                <td>$").append(String.format("%,.2f", totalValue)).append("</td>\n");
            html.append("                <td></td>\n");
            html.append("            </tr>\n");
        } else {
            html.append("            <tr>\n");
            html.append("                <td colspan=\"5\">No holdings data available</td>\n");
            html.append("            </tr>\n");
        }
        
        html.append("        </tbody>\n");
        html.append("    </table>\n");
        
        // Footer
        html.append("    <p><em>Report generated on: ").append(java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))).append("</em></p>\n");
        html.append("</body>\n");
        html.append("</html>\n");
        
        return html.toString();
    }
    
    /**
     * Escapes HTML special characters
     *
     * @param text The text to escape
     * @return The escaped text
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#x27;");
    }
}