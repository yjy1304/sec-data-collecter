package com.company.sec13f.service;

import com.company.sec13f.repository.database.FilingDAO;
import com.company.sec13f.repository.model.Filing;
import com.company.sec13f.repository.model.Holding;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class HoldingAnalysisService {
    private final FilingDAO filingDAO;
    
    @Autowired
    public HoldingAnalysisService(FilingDAO filingDAO) {
        this.filingDAO = filingDAO;
    }
    
    public InstitutionAnalysis analyzeInstitution(String cik) throws SQLException {
        List<Filing> filings = filingDAO.getFilingsByCik(cik);
        if (filings.isEmpty()) {
            throw new IllegalArgumentException("No filings found for CIK: " + cik);
        }
        
        List<FilingWithHoldings> filingsWithHoldings = new ArrayList<>();
        
        for (Filing filing : filings) {
            List<Holding> holdings = filingDAO.getHoldingsByFilingId(
                filingDAO.getFilingIdByAccessionNumber(filing.getAccessionNumber()));
            filing.setHoldings(holdings);
            filingsWithHoldings.add(new FilingWithHoldings(filing, holdings));
        }
        
        return new InstitutionAnalysis(cik, filings.get(0).getCompanyName(), filingsWithHoldings);
    }
    
    public List<TopHolding> getTopHoldingsByValue(String cik, int limit) throws SQLException {
        List<FilingDAO.HoldingWithFilingInfo> allHoldings = filingDAO.getAllHoldingsByCik(cik);
        
        return allHoldings.stream()
                .collect(Collectors.groupingBy(h -> h.getHolding().getCusip()))
                .entrySet().stream()
                .map(entry -> {
                    String cusip = entry.getKey();
                    List<FilingDAO.HoldingWithFilingInfo> holdingHistory = entry.getValue();
                    
                    FilingDAO.HoldingWithFilingInfo latest = holdingHistory.get(0);
                    
                    return new TopHolding(
                            latest.getHolding().getNameOfIssuer(),
                            cusip,
                            latest.getHolding().getValue(),
                            latest.getHolding().getShares(),
                            latest.getFilingDate()
                    );
                })
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    public List<HoldingTrend> getHoldingTrends(String cik, String cusip) throws SQLException {
        List<FilingDAO.HoldingWithFilingInfo> allHoldings = filingDAO.getAllHoldingsByCik(cik);
        
        return allHoldings.stream()
                .filter(h -> cusip.equals(h.getHolding().getCusip()))
                .map(h -> new HoldingTrend(
                        h.getFilingDate(),
                        h.getHolding().getValue(),
                        h.getHolding().getShares()
                ))
                .sorted(Comparator.comparing(HoldingTrend::getFilingDate))
                .collect(Collectors.toList());
    }
    
    public PortfolioSummary getPortfolioSummary(String cik) throws SQLException {
        List<Filing> filings = filingDAO.getFilingsByCik(cik);
        if (filings.isEmpty()) {
            return null;
        }
        
        Filing latestFiling = filings.get(0);
        List<Holding> latestHoldings = filingDAO.getHoldingsByFilingId(
            filingDAO.getFilingIdByAccessionNumber(latestFiling.getAccessionNumber()));
        
        BigDecimal totalValue = latestHoldings.stream()
                .map(Holding::getValue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        long totalShares = latestHoldings.stream()
                .map(Holding::getShares)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .sum();
        
        Map<String, BigDecimal> sectorAllocation = calculateSectorAllocation(latestHoldings);
        
        return new PortfolioSummary(
                latestFiling.getFilingDate(),
                latestHoldings.size(),
                totalValue,
                totalShares,
                sectorAllocation
        );
    }
    
    public List<HoldingChange> getHoldingChanges(String cik) throws SQLException {
        List<Filing> filings = filingDAO.getFilingsByCik(cik);
        if (filings.size() < 2) {
            return Collections.emptyList();
        }
        
        Filing currentFiling = filings.get(0);
        Filing previousFiling = filings.get(1);
        
        List<Holding> currentHoldings = filingDAO.getHoldingsByFilingId(
            filingDAO.getFilingIdByAccessionNumber(currentFiling.getAccessionNumber()));
        List<Holding> previousHoldings = filingDAO.getHoldingsByFilingId(
            filingDAO.getFilingIdByAccessionNumber(previousFiling.getAccessionNumber()));
        
        Map<String, Holding> currentMap = currentHoldings.stream()
                .collect(Collectors.toMap(Holding::getCusip, h -> h));
        Map<String, Holding> previousMap = previousHoldings.stream()
                .collect(Collectors.toMap(Holding::getCusip, h -> h));
        
        List<HoldingChange> changes = new ArrayList<>();
        
        Set<String> allCusips = new HashSet<>();
        allCusips.addAll(currentMap.keySet());
        allCusips.addAll(previousMap.keySet());
        
        for (String cusip : allCusips) {
            Holding current = currentMap.get(cusip);
            Holding previous = previousMap.get(cusip);
            
            if (current != null && previous != null) {
                BigDecimal valueChange = current.getValue().subtract(previous.getValue());
                Long shareChange = current.getShares() - previous.getShares();
                
                if (valueChange.compareTo(BigDecimal.ZERO) != 0 || !shareChange.equals(0L)) {
                    changes.add(new HoldingChange(
                            current.getNameOfIssuer(),
                            cusip,
                            HoldingChange.ChangeType.MODIFIED,
                            previous.getValue(),
                            current.getValue(),
                            valueChange,
                            previous.getShares(),
                            current.getShares(),
                            shareChange
                    ));
                }
            } else if (current != null) {
                changes.add(new HoldingChange(
                        current.getNameOfIssuer(),
                        cusip,
                        HoldingChange.ChangeType.NEW,
                        BigDecimal.ZERO,
                        current.getValue(),
                        current.getValue(),
                        0L,
                        current.getShares(),
                        current.getShares()
                ));
            } else {
                changes.add(new HoldingChange(
                        previous.getNameOfIssuer(),
                        cusip,
                        HoldingChange.ChangeType.SOLD,
                        previous.getValue(),
                        BigDecimal.ZERO,
                        previous.getValue().negate(),
                        previous.getShares(),
                        0L,
                        -previous.getShares()
                ));
            }
        }
        
        return changes.stream()
                .sorted((a, b) -> b.getValueChange().abs().compareTo(a.getValueChange().abs()))
                .collect(Collectors.toList());
    }
    
    private Map<String, BigDecimal> calculateSectorAllocation(List<Holding> holdings) {
        Map<String, BigDecimal> sectorMap = new HashMap<>();
        
        BigDecimal totalValue = holdings.stream()
                .map(Holding::getValue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        for (Holding holding : holdings) {
            String sector = determineSector(holding.getNameOfIssuer());
            BigDecimal percentage = holding.getValue()
                    .divide(totalValue, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            
            sectorMap.merge(sector, percentage, BigDecimal::add);
        }
        
        return sectorMap;
    }
    
    private String determineSector(String issuerName) {
        String name = issuerName.toLowerCase();
        
        if (name.contains("tech") || name.contains("software") || name.contains("microsoft") || 
            name.contains("apple") || name.contains("google") || name.contains("amazon")) {
            return "Technology";
        } else if (name.contains("bank") || name.contains("financial") || name.contains("capital")) {
            return "Financial";
        } else if (name.contains("health") || name.contains("pharma") || name.contains("bio")) {
            return "Healthcare";
        } else if (name.contains("energy") || name.contains("oil") || name.contains("gas")) {
            return "Energy";
        } else if (name.contains("retail") || name.contains("consumer")) {
            return "Consumer";
        }
        
        return "Other";
    }
    
    
    public static class InstitutionAnalysis {
        private final String cik;
        private final String institutionName;
        private final List<FilingWithHoldings> filings;
        
        public InstitutionAnalysis(String cik, String institutionName, List<FilingWithHoldings> filings) {
            this.cik = cik;
            this.institutionName = institutionName;
            this.filings = filings;
        }
        
        public String getCik() { return cik; }
        public String getInstitutionName() { return institutionName; }
        public List<FilingWithHoldings> getFilings() { return filings; }
    }
    
    public static class FilingWithHoldings {
        private final Filing filing;
        private final List<Holding> holdings;
        
        public FilingWithHoldings(Filing filing, List<Holding> holdings) {
            this.filing = filing;
            this.holdings = holdings;
        }
        
        public Filing getFiling() { return filing; }
        public List<Holding> getHoldings() { return holdings; }
    }
    
    public static class TopHolding {
        private final String nameOfIssuer;
        private final String cusip;
        private final BigDecimal value;
        private final Long shares;
        private final LocalDate asOfDate;
        
        public TopHolding(String nameOfIssuer, String cusip, BigDecimal value, Long shares, LocalDate asOfDate) {
            this.nameOfIssuer = nameOfIssuer;
            this.cusip = cusip;
            this.value = value;
            this.shares = shares;
            this.asOfDate = asOfDate;
        }
        
        public String getNameOfIssuer() { return nameOfIssuer; }
        public String getCusip() { return cusip; }
        public BigDecimal getValue() { return value; }
        public Long getShares() { return shares; }
        public LocalDate getAsOfDate() { return asOfDate; }
    }
    
    public static class HoldingTrend {
        private final LocalDate filingDate;
        private final BigDecimal value;
        private final Long shares;
        
        public HoldingTrend(LocalDate filingDate, BigDecimal value, Long shares) {
            this.filingDate = filingDate;
            this.value = value;
            this.shares = shares;
        }
        
        public LocalDate getFilingDate() { return filingDate; }
        public BigDecimal getValue() { return value; }
        public Long getShares() { return shares; }
    }
    
    public static class PortfolioSummary {
        private final LocalDate asOfDate;
        private final int numberOfHoldings;
        private final BigDecimal totalValue;
        private final long totalShares;
        private final Map<String, BigDecimal> sectorAllocation;
        
        public PortfolioSummary(LocalDate asOfDate, int numberOfHoldings, BigDecimal totalValue, 
                              long totalShares, Map<String, BigDecimal> sectorAllocation) {
            this.asOfDate = asOfDate;
            this.numberOfHoldings = numberOfHoldings;
            this.totalValue = totalValue;
            this.totalShares = totalShares;
            this.sectorAllocation = sectorAllocation;
        }
        
        public LocalDate getAsOfDate() { return asOfDate; }
        public int getNumberOfHoldings() { return numberOfHoldings; }
        public BigDecimal getTotalValue() { return totalValue; }
        public long getTotalShares() { return totalShares; }
        public Map<String, BigDecimal> getSectorAllocation() { return sectorAllocation; }
    }
    
    public static class HoldingChange {
        public enum ChangeType { NEW, SOLD, MODIFIED }
        
        private final String nameOfIssuer;
        private final String cusip;
        private final ChangeType changeType;
        private final BigDecimal previousValue;
        private final BigDecimal currentValue;
        private final BigDecimal valueChange;
        private final Long previousShares;
        private final Long currentShares;
        private final Long shareChange;
        
        public HoldingChange(String nameOfIssuer, String cusip, ChangeType changeType,
                           BigDecimal previousValue, BigDecimal currentValue, BigDecimal valueChange,
                           Long previousShares, Long currentShares, Long shareChange) {
            this.nameOfIssuer = nameOfIssuer;
            this.cusip = cusip;
            this.changeType = changeType;
            this.previousValue = previousValue;
            this.currentValue = currentValue;
            this.valueChange = valueChange;
            this.previousShares = previousShares;
            this.currentShares = currentShares;
            this.shareChange = shareChange;
        }
        
        public String getNameOfIssuer() { return nameOfIssuer; }
        public String getCusip() { return cusip; }
        public ChangeType getChangeType() { return changeType; }
        public BigDecimal getPreviousValue() { return previousValue; }
        public BigDecimal getCurrentValue() { return currentValue; }
        public BigDecimal getValueChange() { return valueChange; }
        public Long getPreviousShares() { return previousShares; }
        public Long getCurrentShares() { return currentShares; }
        public Long getShareChange() { return shareChange; }
    }
}