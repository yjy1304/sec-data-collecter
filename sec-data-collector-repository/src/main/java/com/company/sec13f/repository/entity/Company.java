package com.company.sec13f.repository.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Company实体类 - 公司维护信息
 * 对应数据库表：companies
 */
public class Company {
    
    private Long id;
    private String cik;
    private String companyName;
    private String displayName;
    private String industry;
    private String sector;
    private String description;
    private String website;
    private Boolean isActive;
    private LocalDate lastFilingDate;
    private Integer totalFilings;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 构造方法
    public Company() {
    }
    
    public Company(String cik, String companyName) {
        this.cik = cik;
        this.companyName = companyName;
        this.isActive = true;
        this.totalFilings = 0;
    }
    
    public Company(String cik, String companyName, String displayName, String industry, String sector) {
        this.cik = cik;
        this.companyName = companyName;
        this.displayName = displayName;
        this.industry = industry;
        this.sector = sector;
        this.isActive = true;
        this.totalFilings = 0;
    }
    
    // Getter和Setter方法
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getCik() {
        return cik;
    }
    
    public void setCik(String cik) {
        this.cik = cik;
    }
    
    public String getCompanyName() {
        return companyName;
    }
    
    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getIndustry() {
        return industry;
    }
    
    public void setIndustry(String industry) {
        this.industry = industry;
    }
    
    public String getSector() {
        return sector;
    }
    
    public void setSector(String sector) {
        this.sector = sector;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getWebsite() {
        return website;
    }
    
    public void setWebsite(String website) {
        this.website = website;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public LocalDate getLastFilingDate() {
        return lastFilingDate;
    }
    
    public void setLastFilingDate(LocalDate lastFilingDate) {
        this.lastFilingDate = lastFilingDate;
    }
    
    public Integer getTotalFilings() {
        return totalFilings;
    }
    
    public void setTotalFilings(Integer totalFilings) {
        this.totalFilings = totalFilings;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    @Override
    public String toString() {
        return "Company{" +
                "id=" + id +
                ", cik='" + cik + '\'' +
                ", companyName='" + companyName + '\'' +
                ", displayName='" + displayName + '\'' +
                ", industry='" + industry + '\'' +
                ", sector='" + sector + '\'' +
                ", isActive=" + isActive +
                ", totalFilings=" + totalFilings +
                ", lastFilingDate=" + lastFilingDate +
                '}';
    }
}