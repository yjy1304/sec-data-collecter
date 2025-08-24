package com.company.sec13f.parser;

import com.company.sec13f.parser.scraper.SECScraper;

import java.io.IOException;

public class Alibaba13FParserTest {
    
    // 阿里巴巴控股集团的CIK号码
    private static final String ALIBABA_CIK = "0001524258";
    
    public static void main(String[] args) {
        System.out.println("开始获取阿里巴巴控股集团的SEC 13F文件...");
        
        try {
            SECScraper scraper = new SECScraper();
            
            // 搜索阿里巴巴的13F文件
            System.out.println("正在搜索CIK为 " + ALIBABA_CIK + " 的13F文件...");
            String searchResult = scraper.searchFilings(ALIBABA_CIK, "13F");
            System.out.println("搜索结果预览（前500个字符）:");
            System.out.println(searchResult != null ? searchResult.substring(0, Math.min(500, searchResult.length())) : "无结果");
            
            System.out.println("\n注意：完整实现需要解析HTML结果以提取具体的文件访问编号。");
            System.out.println("在实际应用中，我们还需要解析搜索结果页面以获取具体的accession number，");
            System.out.println("然后使用该编号获取详细的13F文件内容。");
            
        } catch (IOException e) {
            System.err.println("获取SEC文件时出错: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("执行过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
