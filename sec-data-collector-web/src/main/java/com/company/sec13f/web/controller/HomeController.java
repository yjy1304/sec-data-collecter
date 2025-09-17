package com.company.sec13f.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Spring MVC Controller for serving static pages
 */
@Controller
public class HomeController {
    
    /**
     * 主页
     */
    @GetMapping("/")
    public String home() {
        return "redirect:/index.html";
    }
    
    /**
     * 分析页面
     */
    @GetMapping("/analysis")
    public String analysis() {
        return "redirect:/analysis.html";
    }
    
    /**
     * 数据抓取页面
     */
    @GetMapping("/scraping")
    public String scraping() {
        return "redirect:/scraping.html";
    }
    
    /**
     * 数据库管理页面
     */
    @GetMapping("/database")
    public String database() {
        return "redirect:/database.html";
    }
    
    /**
     * 日志页面
     */
    @GetMapping("/logs")
    public String logs() {
        return "redirect:/logs.html";
    }
}