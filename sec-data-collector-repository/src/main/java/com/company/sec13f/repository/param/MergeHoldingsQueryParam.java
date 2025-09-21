package com.company.sec13f.repository.param;

/**
 * 合并持仓查询参数类
 */
public class MergeHoldingsQueryParam {
    
    private String cik;
    private Double minValue;
    private String search;
    private String sortBy;
    private String sortOrder;
    private String reportPeriodFrom;
    private String reportPeriodTo;
    
    public MergeHoldingsQueryParam() {
    }
    
    public MergeHoldingsQueryParam(String cik, Double minValue, String search, String sortBy, String sortOrder, String reportPeriodFrom, String reportPeriodTo) {
        this.cik = cik;
        this.minValue = minValue;
        this.search = search;
        this.sortBy = sortBy;
        this.sortOrder = sortOrder;
        this.reportPeriodFrom = reportPeriodFrom;
        this.reportPeriodTo = reportPeriodTo;
    }
    
    public String getCik() {
        return cik;
    }
    
    public void setCik(String cik) {
        this.cik = cik;
    }
    
    public Double getMinValue() {
        return minValue;
    }
    
    public void setMinValue(Double minValue) {
        this.minValue = minValue;
    }
    
    public String getSearch() {
        return search;
    }
    
    public void setSearch(String search) {
        this.search = search;
    }
    
    public String getSortBy() {
        return sortBy;
    }
    
    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }
    
    public String getSortOrder() {
        return sortOrder;
    }
    
    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }
    
    public String getReportPeriodFrom() {
        return reportPeriodFrom;
    }
    
    public void setReportPeriodFrom(String reportPeriodFrom) {
        this.reportPeriodFrom = reportPeriodFrom;
    }
    
    public String getReportPeriodTo() {
        return reportPeriodTo;
    }
    
    public void setReportPeriodTo(String reportPeriodTo) {
        this.reportPeriodTo = reportPeriodTo;
    }
}