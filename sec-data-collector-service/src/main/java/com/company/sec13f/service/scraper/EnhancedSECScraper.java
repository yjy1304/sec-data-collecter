package com.company.sec13f.service.scraper;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enhanced SEC scraper that can retrieve real 13F filings from the SEC website
 */
public class EnhancedSECScraper {
    private static final String SEC_BASE_URL = "https://www.sec.gov";
    private static final String SEC_SEARCH_URL = SEC_BASE_URL + "/cgi-bin/browse-edgar";
    private static final String SEC_ARCHIVES_BASE_URL = "https://www.sec.gov/Archives";
    
    private final HttpClient httpClient;

    public EnhancedSECScraper() {
        this.httpClient = HttpClientBuilder.create().build();
    }

    /**
     * Searches for filings for a given CIK number
     *
     * @param cik The CIK number of the company
     * @param filingType The type of filing to search for (e.g., 13F)
     * @return The HTML response from the SEC website
     * @throws IOException If an error occurs during the HTTP request
     */
    public String searchFilings(String cik, String filingType) throws IOException {
        String url = SEC_SEARCH_URL + "?CIK=" + cik + "&type=" + filingType + "&owner=include";
        return executeGetRequest(url);
    }

    /**
     * Retrieves the filing details for a given accession number
     *
     * @param cik The CIK number of the company
     * @param accessionNumber The accession number of the filing
     * @return The HTML response from the SEC website
     * @throws IOException If an error occurs during the HTTP request
     */
    public String getFilingDetails(String cik, String accessionNumber) throws IOException {
        String url = SEC_ARCHIVES_BASE_URL + "/edgar/data/" + cik + "/" + accessionNumber.replace("-", "") + "/" + accessionNumber + ".txt";
        return executeGetRequest(url);
    }

    /**
     * Retrieves the raw XML content of a filing
     *
     * @param cik The CIK number of the company
     * @param accessionNumber The accession number of the filing
     * @return The XML content of the filing
     * @throws IOException If an error occurs during the HTTP request
     */
    public String getFilingXML(String cik, String accessionNumber) throws IOException {
        String url = SEC_ARCHIVES_BASE_URL + "/edgar/data/" + cik + "/" + accessionNumber.replace("-", "") + "/" + accessionNumber + "-index.html";
        String indexPage = executeGetRequest(url);
        
        // Extract the XML file URL from the index page
        String xmlUrl = extractXmlUrlFromIndex(indexPage);
        if (xmlUrl != null) {
            return executeGetRequest(SEC_ARCHIVES_BASE_URL + xmlUrl);
        }
        
        return null;
    }
    
    /**
     * Extracts the accession number from the search results page
     *
     * @param searchResults The HTML content of the search results page
     * @return The accession number, or null if not found
     */
    public String extractAccessionNumber(String searchResults) {
        // Look for accession number in the search results
        Pattern pattern = Pattern.compile("Accession Number:</td><td[^>]*>([\\d-]+)</td>");
        Matcher matcher = pattern.matcher(searchResults);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }

    /**
     * Executes an HTTP GET request and returns the response content
     *
     * @param url The URL to request
     * @return The response content
     * @throws IOException If an error occurs during the HTTP request
     */
    private String executeGetRequest(String url) throws IOException {
        HttpGet request = new HttpGet(url);
        // Set user agent to comply with SEC website requirements
        request.setHeader("User-Agent", "SEC13FParser Mozilla/5.0");
        // Set accept header
        request.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        
        HttpResponse response = httpClient.execute(request);
        HttpEntity entity = response.getEntity();
        
        if (entity != null) {
            return EntityUtils.toString(entity);
        }
        
        return null;
    }

    /**
     * Extracts the XML file URL from the filing index page
     *
     * @param indexPage The HTML content of the index page
     * @return The URL of the XML file, or null if not found
     */
    private String extractXmlUrlFromIndex(String indexPage) {
        // Look for the XML file link in the index page
        int start = indexPage.indexOf(".xml");
        if (start > 0) {
            // Find the beginning of the href attribute
            int hrefStart = indexPage.lastIndexOf("href=\"", start);
            if (hrefStart > 0) {
                hrefStart += 6; // Move past 'href="'
                return indexPage.substring(hrefStart, start + 4); // +4 for ".xml"
            }
        }
        return null;
    }
}