package com.company.sec13f.parser.util;

import com.company.sec13f.parser.model.Filing;
import com.company.sec13f.parser.model.Holding;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class DataValidator {
    
    private static final Pattern CIK_PATTERN = Pattern.compile("^\\d{10}$");
    private static final Pattern CUSIP_PATTERN = Pattern.compile("^[0-9A-Z]{9}$");
    private static final Pattern ACCESSION_PATTERN = Pattern.compile("^\\d{10}-\\d{2}-\\d{6}$");
    
    /**
     * 验证并清理Filing对象
     */
    public static ValidationResult validateFiling(Filing filing) {
        ValidationResult result = new ValidationResult();
        
        if (filing == null) {
            result.addError("Filing object is null");
            return result;
        }
        
        // 验证CIK
        if (filing.getCik() == null || filing.getCik().trim().isEmpty()) {
            result.addError("CIK is required");
        } else {
            String cleanCik = cleanCik(filing.getCik());
            if (!CIK_PATTERN.matcher(cleanCik).matches()) {
                result.addError("Invalid CIK format: " + filing.getCik());
            } else {
                filing.setCik(cleanCik);
            }
        }
        
        // 验证公司名称
        if (filing.getCompanyName() == null || filing.getCompanyName().trim().isEmpty()) {
            result.addWarning("Company name is missing");
            filing.setCompanyName("Unknown Company");
        } else {
            filing.setCompanyName(cleanCompanyName(filing.getCompanyName()));
        }
        
        // 验证文件类型
        if (filing.getFilingType() == null || filing.getFilingType().trim().isEmpty()) {
            result.addWarning("Filing type is missing, defaulting to 13F-HR");
            filing.setFilingType("13F-HR");
        } else if (!isValid13FType(filing.getFilingType())) {
            result.addWarning("Unusual filing type: " + filing.getFilingType());
        }
        
        // 验证日期
        if (filing.getFilingDate() == null) {
            result.addError("Filing date is required");
        } else if (filing.getFilingDate().isAfter(LocalDate.now())) {
            result.addError("Filing date cannot be in the future: " + filing.getFilingDate());
        } else if (filing.getFilingDate().isBefore(LocalDate.of(1993, 1, 1))) {
            result.addWarning("Filing date is very old: " + filing.getFilingDate());
        }
        
        // 验证Accession Number
        if (filing.getAccessionNumber() == null || filing.getAccessionNumber().trim().isEmpty()) {
            result.addError("Accession number is required");
        } else if (!ACCESSION_PATTERN.matcher(filing.getAccessionNumber()).matches()) {
            result.addError("Invalid accession number format: " + filing.getAccessionNumber());
        }
        
        // 验证持仓数据
        if (filing.getHoldings() != null) {
            List<Holding> validHoldings = new ArrayList<>();
            for (int i = 0; i < filing.getHoldings().size(); i++) {
                Holding holding = filing.getHoldings().get(i);
                ValidationResult holdingResult = validateHolding(holding, i);
                result.addSubResult("Holding " + i, holdingResult);
                
                if (!holdingResult.hasErrors()) {
                    validHoldings.add(holding);
                }
            }
            filing.setHoldings(validHoldings);
            
            if (validHoldings.isEmpty() && !filing.getHoldings().isEmpty()) {
                result.addWarning("All holdings were invalid and removed");
            }
        }
        
        return result;
    }
    
    /**
     * 验证单个持仓对象
     */
    public static ValidationResult validateHolding(Holding holding, int index) {
        ValidationResult result = new ValidationResult();
        
        if (holding == null) {
            result.addError("Holding object is null");
            return result;
        }
        
        // 验证发行人名称
        if (holding.getNameOfIssuer() == null || holding.getNameOfIssuer().trim().isEmpty()) {
            result.addError("Issuer name is required");
        } else {
            holding.setNameOfIssuer(cleanIssuerName(holding.getNameOfIssuer()));
        }
        
        // 验证CUSIP
        if (holding.getCusip() == null || holding.getCusip().trim().isEmpty()) {
            result.addError("CUSIP is required");
        } else {
            String cleanCusip = cleanCusip(holding.getCusip());
            if (!CUSIP_PATTERN.matcher(cleanCusip).matches()) {
                result.addError("Invalid CUSIP format: " + holding.getCusip());
            } else {
                holding.setCusip(cleanCusip);
            }
        }
        
        // 验证市值
        if (holding.getValue() == null) {
            result.addError("Holding value is required");
        } else if (holding.getValue().compareTo(BigDecimal.ZERO) < 0) {
            result.addError("Holding value cannot be negative: " + holding.getValue());
        } else if (holding.getValue().compareTo(new BigDecimal("1000000000000")) > 0) {
            result.addWarning("Holding value is unusually large: " + holding.getValue());
        }
        
        // 验证股数
        if (holding.getShares() == null) {
            result.addWarning("Share count is missing");
            holding.setShares(0L);
        } else if (holding.getShares() < 0) {
            result.addError("Share count cannot be negative: " + holding.getShares());
        } else if (holding.getShares() > 10000000000L) {
            result.addWarning("Share count is unusually large: " + holding.getShares());
        }
        
        return result;
    }
    
    /**
     * 清理CIK - 转换为10位数字格式
     */
    public static String cleanCik(String cik) {
        if (cik == null) return null;
        
        String cleaned = cik.replaceAll("[^0-9]", "");
        if (cleaned.length() <= 10) {
            return String.format("%010d", Long.parseLong(cleaned));
        }
        return cleaned;
    }
    
    /**
     * 清理CUSIP - 转换为9位大写字母数字格式
     */
    public static String cleanCusip(String cusip) {
        if (cusip == null) return null;
        
        return cusip.trim().toUpperCase().replaceAll("[^0-9A-Z]", "");
    }
    
    /**
     * 清理公司名称
     */
    public static String cleanCompanyName(String name) {
        if (name == null) return null;
        
        return name.trim()
                .replaceAll("\\s+", " ")
                .replaceAll("[\\x00-\\x1f\\x7f]", ""); // 移除控制字符
    }
    
    /**
     * 清理发行人名称
     */
    public static String cleanIssuerName(String name) {
        if (name == null) return null;
        
        return name.trim()
                .replaceAll("\\s+", " ")
                .replaceAll("[\\x00-\\x1f\\x7f]", "") // 移除控制字符
                .replaceAll("(?i)\\b(inc|corp|co|ltd|llc|company)\\b\\.?$", "$1"); // 标准化公司后缀
    }
    
    /**
     * 验证是否为有效的13F文件类型
     */
    private static boolean isValid13FType(String filingType) {
        return filingType.matches("(?i)^13F(-HR|-HR/A)?$");
    }
    
    /**
     * 验证结果类
     */
    public static class ValidationResult {
        private final List<String> errors;
        private final List<String> warnings;
        private final List<SubResult> subResults;
        
        public ValidationResult() {
            this.errors = new ArrayList<>();
            this.warnings = new ArrayList<>();
            this.subResults = new ArrayList<>();
        }
        
        public void addError(String error) {
            errors.add(error);
        }
        
        public void addWarning(String warning) {
            warnings.add(warning);
        }
        
        public void addSubResult(String name, ValidationResult subResult) {
            subResults.add(new SubResult(name, subResult));
        }
        
        public boolean hasErrors() {
            if (!errors.isEmpty()) return true;
            return subResults.stream().anyMatch(sub -> sub.result.hasErrors());
        }
        
        public boolean hasWarnings() {
            if (!warnings.isEmpty()) return true;
            return subResults.stream().anyMatch(sub -> sub.result.hasWarnings());
        }
        
        public List<String> getErrors() { return new ArrayList<>(errors); }
        public List<String> getWarnings() { return new ArrayList<>(warnings); }
        public List<SubResult> getSubResults() { return new ArrayList<>(subResults); }
        
        public List<String> getAllErrors() {
            List<String> allErrors = new ArrayList<>(errors);
            for (SubResult sub : subResults) {
                for (String error : sub.result.getAllErrors()) {
                    allErrors.add(sub.name + ": " + error);
                }
            }
            return allErrors;
        }
        
        public List<String> getAllWarnings() {
            List<String> allWarnings = new ArrayList<>(warnings);
            for (SubResult sub : subResults) {
                for (String warning : sub.result.getAllWarnings()) {
                    allWarnings.add(sub.name + ": " + warning);
                }
            }
            return allWarnings;
        }
        
        public boolean isValid() {
            return !hasErrors();
        }
        
        public String getSummary() {
            int totalErrors = getAllErrors().size();
            int totalWarnings = getAllWarnings().size();
            
            if (totalErrors == 0 && totalWarnings == 0) {
                return "Validation passed";
            } else if (totalErrors == 0) {
                return "Validation passed with " + totalWarnings + " warnings";
            } else {
                return "Validation failed with " + totalErrors + " errors and " + totalWarnings + " warnings";
            }
        }
        
        public static class SubResult {
            private final String name;
            private final ValidationResult result;
            
            public SubResult(String name, ValidationResult result) {
                this.name = name;
                this.result = result;
            }
            
            public String getName() { return name; }
            public ValidationResult getResult() { return result; }
        }
    }
}