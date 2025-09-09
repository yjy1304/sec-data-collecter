package com.company.sec13f.repository.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SEC 13F Holding实体类
 * 对应数据库表：holdings
 */
public class Holding {
    
    private Long id;
    private Long filingId;
    private String cik;
    private String companyName;
    private String nameOfIssuer;
    private String cusip;
    private BigDecimal value;
    private Long shares;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 关联的Filing信息（用于联合查询）
    private Filing filing;
    
    // 公司显示名称（从companies表关联获取）
    private String companyDisplayName;
    
    // 构造方法
    public Holding() {
    }
    
    public Holding(Long filingId, String cik, String companyName, String nameOfIssuer, String cusip, 
                   BigDecimal value, Long shares) {
        this.filingId = filingId;
        this.cik = cik;
        this.companyName = companyName;
        this.nameOfIssuer = nameOfIssuer;
        this.cusip = cusip;
        this.value = value;
        this.shares = shares;
    }
    
    // Getter和Setter方法
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getFilingId() {
        return filingId;
    }
    
    public void setFilingId(Long filingId) {
        this.filingId = filingId;
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
    
    public String getNameOfIssuer() {
        return nameOfIssuer;
    }
    
    public void setNameOfIssuer(String nameOfIssuer) {
        this.nameOfIssuer = nameOfIssuer;
    }
    
    public String getCusip() {
        return cusip;
    }
    
    public void setCusip(String cusip) {
        this.cusip = cusip;
    }
    
    public BigDecimal getValue() {
        return value;
    }
    
    public void setValue(BigDecimal value) {
        this.value = value;
    }
    
    public Long getShares() {
        return shares;
    }
    
    public void setShares(Long shares) {
        this.shares = shares;
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
    
    public Filing getFiling() {
        return filing;
    }
    
    public void setFiling(Filing filing) {
        this.filing = filing;
    }
    
    public String getCompanyDisplayName() {
        return companyDisplayName;
    }
    
    public void setCompanyDisplayName(String companyDisplayName) {
        this.companyDisplayName = companyDisplayName;
    }
    
    @Override
    public String toString() {
        return "Holding{" +
                "id=" + id +
                ", filingId=" + filingId +
                ", cik='" + cik + '\'' +
                ", companyName='" + companyName + '\'' +
                ", nameOfIssuer='" + nameOfIssuer + '\'' +
                ", cusip='" + cusip + '\'' +
                ", value=" + value +
                ", shares=" + shares +
                '}';
    }
}