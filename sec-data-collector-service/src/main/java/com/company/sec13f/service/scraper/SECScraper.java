package com.company.sec13f.service.scraper;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public class SECScraper {
    private static final String SEC_BASE_URL = "https://www.sec.gov";
    private static final String SEC_SEARCH_URL = SEC_BASE_URL + "/cgi-bin/browse-edgar";
    private static final String SEC_ARCHIVES_BASE_URL = "https://www.sec.gov/Archives";
    
    private final HttpClient httpClient;

    public SECScraper() {
        this.httpClient = HttpClientBuilder.create().build();
    }

    public String searchFilings(String cik, String filingType) throws IOException {
        String url = SEC_SEARCH_URL + "?CIK=" + cik + "&type=" + filingType + "&owner=include";
        return executeGetRequest(url);
    }

    public String getFilingDetails(String cik, String accessionNumber) throws IOException {
        String url = SEC_ARCHIVES_BASE_URL + "/edgar/data/" + cik + "/" + accessionNumber.replace("-", "") + "/" + accessionNumber + ".txt";
        return executeGetRequest(url);
    }

    public String getFilingXML(String cik, String accessionNumber) throws IOException {
        String url = SEC_ARCHIVES_BASE_URL + "/edgar/data/" + cik + "/" + accessionNumber.replace("-", "") + "/" + accessionNumber + "-index.html";
        String indexPage = executeGetRequest(url);
        
        String xmlUrl = extractXmlUrlFromIndex(indexPage);
        if (xmlUrl != null) {
            return executeGetRequest(SEC_ARCHIVES_BASE_URL + xmlUrl);
        }
        
        return null;
    }

    private String executeGetRequest(String url) throws IOException {
        HttpGet request = new HttpGet(url);
        request.setHeader("User-Agent", "SEC13FParser Mozilla/5.0");
        
        HttpResponse response = httpClient.execute(request);
        HttpEntity entity = response.getEntity();
        
        if (entity != null) {
            return EntityUtils.toString(entity);
        }
        
        return null;
    }

    private String extractXmlUrlFromIndex(String indexPage) {
        int start = indexPage.indexOf(".xml");
        if (start > 0) {
            int hrefStart = indexPage.lastIndexOf("href=\"", start);
            if (hrefStart > 0) {
                hrefStart += 6;
                return indexPage.substring(hrefStart, start + 4);
            }
        }
        return null;
    }
}
