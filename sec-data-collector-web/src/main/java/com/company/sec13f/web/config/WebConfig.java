package com.company.sec13f.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

/**
 * Spring MVC Web Configuration
 */
@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {
    
    /**
     * 配置静态资源处理
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/webapp/", "classpath:/static/", "classpath:/public/")
                .setCachePeriod(3600);
                
        // 特殊处理CSS和JS文件
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/webapp/css/")
                .setCachePeriod(86400); // 24小时缓存
                
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/webapp/js/")
                .setCachePeriod(86400); // 24小时缓存
                
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/webapp/images/")
                .setCachePeriod(604800); // 7天缓存
    }
    
    /**
     * 配置视图解析器
     */
    @Override
    public void configureViewResolvers(ViewResolverRegistry registry) {
        registry.enableContentNegotiation();
    }
    
    /**
     * 配置CORS
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
    
    /**
     * 配置默认Servlet处理 - 注释掉，Spring Boot自动处理静态资源
     */
    // @Override
    // public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
    //     configurer.enable();
    // }
}