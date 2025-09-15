package com.company.sec13f.repository.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 合并持仓实体类
 * 对应 merge_holdings 表
 */
public class MergeHolding {
    
    private Long id;
    private Long holdingId;
    private Long filingId;
    private String cik;
    private String companyName;
    private String nameOfIssuer;
    private String cusip;
    private BigDecimal value;
    private Long shares;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 构造函数
    public MergeHolding() {}
    
    public MergeHolding(Long holdingId, Long filingId, String cik, String companyName, 
                       String nameOfIssuer, String cusip, BigDecimal value, Long shares) {
        this.holdingId = holdingId;
        this.filingId = filingId;
        this.cik = cik;
        this.companyName = companyName;
        this.nameOfIssuer = nameOfIssuer;
        this.cusip = cusip;
        this.value = value;
        this.shares = shares;
    }
    
    // Getter and Setter methods
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getHoldingId() {
        return holdingId;
    }
    
    public void setHoldingId(Long holdingId) {
        this.holdingId = holdingId;
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
    
    @Override
    public String toString() {
        return "MergeHolding{" +
                "id=" + id +
                ", holdingId=" + holdingId +
                ", filingId=" + filingId +
                ", cik='" + cik + '\'' +
                ", companyName='" + companyName + '\'' +
                ", nameOfIssuer='" + nameOfIssuer + '\'' +
                ", cusip='" + cusip + '\'' +
                ", value=" + value +
                ", shares=" + shares +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}