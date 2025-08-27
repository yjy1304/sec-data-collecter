package com.company.sec13f.repository.model;

import java.time.LocalDate;
import java.util.List;

/**
 * Represents a 13F filing
 */
public class Filing {
    private String cik;
    private String companyName;
    private String filingType;
    private LocalDate filingDate;
    private String accessionNumber;
    private String formFile;
    private List<Holding> holdings;

    // Constructors
    public Filing() {
    }

    public Filing(String cik, String companyName, String filingType, LocalDate filingDate, String accessionNumber, String formFile) {
        this.cik = cik;
        this.companyName = companyName;
        this.filingType = filingType;
        this.filingDate = filingDate;
        this.accessionNumber = accessionNumber;
        this.formFile = formFile;
    }

    // Getters and Setters
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

    public List<Holding> getHoldings() {
        return holdings;
    }

    public void setHoldings(List<Holding> holdings) {
        this.holdings = holdings;
    }

    @Override
    public String toString() {
        return "Filing{" +
                "cik='" + cik + '\'' +
                ", companyName='" + companyName + '\'' +
                ", filingType='" + filingType + '\'' +
                ", filingDate=" + filingDate +
                ", accessionNumber='" + accessionNumber + '\'' +
                ", formFile='" + formFile + '\'' +
                ", holdingsCount=" + (holdings != null ? holdings.size() : 0) +
                '}';
    }
}