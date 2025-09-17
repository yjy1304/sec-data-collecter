package com.company.sec13f.repository.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * SEC 13F Filing实体类
 * 对应数据库表：filings
 */
public class Filing {
    
    private Long id;
    private String cik;
    private String companyName;
    private String filingType;
    private LocalDate filingDate;
    private String reportPeriod;
    private String accessionNumber;
    private String formFile;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 关联的Holdings（延迟加载）
    private List<Holding> holdings;
    
    // 持仓数量（用于统计查询）
    private Integer holdingsCount;
    
    // 构造方法
    public Filing() {
    }
    
    public Filing(String cik, String companyName, String filingType, 
                 LocalDate filingDate, String accessionNumber, String formFile) {
        this.cik = cik;
        this.companyName = companyName;
        this.filingType = filingType;
        this.filingDate = filingDate;
        this.accessionNumber = accessionNumber;
        this.formFile = formFile;
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
    
    public String getFilingType() {
        return filingType;
    }
    
    public void setFilingType(String filingType) {
        this.filingType = filingType;
    }
    
    public LocalDate getFilingDate() {
        return filingDate;
    }
    
    public void setFilingDate(LocalDate filingDate) {
        this.filingDate = filingDate;
    }
    
    public String getReportPeriod() {
        return reportPeriod;
    }
    
    public void setReportPeriod(String reportPeriod) {
        this.reportPeriod = reportPeriod;
    }
    
    public String getAccessionNumber() {
        return accessionNumber;
    }
    
    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }
    
    public String getFormFile() {
        return formFile;
    }
    
    public void setFormFile(String formFile) {
        this.formFile = formFile;
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
    
    public List<Holding> getHoldings() {
        return holdings;
    }
    
    public void setHoldings(List<Holding> holdings) {
        this.holdings = holdings;
    }
    
    public Integer getHoldingsCount() {
        return holdingsCount;
    }
    
    public void setHoldingsCount(Integer holdingsCount) {
        this.holdingsCount = holdingsCount;
    }
    
    @Override
    public String toString() {
        return "Filing{" +
                "id=" + id +
                ", cik='" + cik + '\'' +
                ", companyName='" + companyName + '\'' +
                ", filingType='" + filingType + '\'' +
                ", filingDate=" + filingDate +
                ", accessionNumber='" + accessionNumber + '\'' +
                ", formFile='" + formFile + '\'' +
                ", holdingsCount=" + (holdings != null ? holdings.size() : 0) +
                '}';
    }
}