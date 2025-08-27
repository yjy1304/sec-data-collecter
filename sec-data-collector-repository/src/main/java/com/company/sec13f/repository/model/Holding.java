package com.company.sec13f.repository.model;

import java.math.BigDecimal;
import java.util.Objects;

public class Holding {
    private String nameOfIssuer;
    private String cusip;
    private BigDecimal value;
    private Long shares;

    public Holding() {}

    public Holding(String nameOfIssuer, String cusip, BigDecimal value, Long shares) {
        this.nameOfIssuer = nameOfIssuer;
        this.cusip = cusip;
        this.value = value;
        this.shares = shares;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Holding holding = (Holding) o;
        return Objects.equals(cusip, holding.cusip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cusip);
    }

    @Override
    public String toString() {
        return "Holding{" +
                "nameOfIssuer='" + nameOfIssuer + '\'' +
                ", cusip='" + cusip + '\'' +
                ", value=" + value +
                ", shares=" + shares +
                '}';
    }
}