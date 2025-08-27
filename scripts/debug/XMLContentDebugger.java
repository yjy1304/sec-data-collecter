package com.company.sec13f.parser.debug;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

public class XMLContentDebugger {
    
    public static void main(String[] args) {
        try {
            HttpClient httpClient = HttpClientBuilder.create()
                .setUserAgent("SEC13F Analysis Tool admin@sec13fparser.com")
                .build();
            
            String url = "https://www.sec.gov/Archives/edgar/data/0001067983/000095012325008361/43981.xml";
            
            HttpGet request = new HttpGet(url);
            request.setHeader("User-Agent", "SEC13F Analysis Tool admin@sec13fparser.com");
            request.setHeader("Accept-Encoding", "gzip, deflate");
            request.setHeader("Accept", "application/json, */*");
            request.setHeader("Host", "www.sec.gov");
            
            HttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            
            System.out.println("Status Code: " + statusCode);
            
            if (statusCode == 200) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    String content = EntityUtils.toString(entity);
                    System.out.println("Content length: " + content.length());
                    System.out.println("\nFirst 2000 characters:");
                    System.out.println(content.substring(0, Math.min(2000, content.length())));
                    
                    System.out.println("\n\nChecking for key elements:");
                    System.out.println("Contains 'informationTable': " + content.contains("informationTable"));
                    System.out.println("Contains 'infoTable': " + content.contains("infoTable"));
                    System.out.println("Contains 'nameOfIssuer': " + content.contains("nameOfIssuer"));
                    System.out.println("Contains 'cusip': " + content.contains("cusip"));
                    System.out.println("Contains '13F': " + content.contains("13F"));
                    
                    // 查找第一个table标签
                    int tableStart = content.indexOf("<table");
                    if (tableStart != -1) {
                        int tableEnd = content.indexOf("</table>", tableStart) + 8;
                        if (tableEnd > tableStart && tableEnd - tableStart < 5000) {
                            System.out.println("\n\nFirst table content:");
                            System.out.println(content.substring(tableStart, Math.min(tableEnd, tableStart + 2000)));
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}