package com.company.sec13f.parser.parser;

import com.company.sec13f.parser.model.Filing;
import com.company.sec13f.parser.model.Holding;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class Enhanced13FXMLParserTest {

    @Test
    public void testParseBasic13FXML() {
        String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<informationTable>" +
            "<infoTable>" +
            "<nameOfIssuer>Apple Inc</nameOfIssuer>" +
            "<cusip>037833100</cusip>" +
            "<value>1000000</value>" +
            "<sshPrnamt>10000</sshPrnamt>" +
            "</infoTable>" +
            "<infoTable>" +
            "<nameOfIssuer>Microsoft Corporation</nameOfIssuer>" +
            "<cusip>594918104</cusip>" +
            "<value>2000000</value>" +
            "<sshPrnamt>20000</sshPrnamt>" +
            "</infoTable>" +
            "</informationTable>";

        Filing filing = Enhanced13FXMLParser.parse13FContent(xmlContent, "test-123", "0001067983");
        
        assertNotNull(filing);
        assertEquals("test-123", filing.getAccessionNumber());
        assertEquals("0001067983", filing.getCik());
        assertEquals("13F-HR", filing.getFilingType());
        
        List<Holding> holdings = filing.getHoldings();
        assertNotNull(holdings);
        assertEquals(2, holdings.size());
        
        Holding apple = holdings.get(0);
        assertEquals("Apple Inc", apple.getNameOfIssuer());
        assertEquals("037833100", apple.getCusip());
        assertEquals(new BigDecimal("1000000"), apple.getValue());
        assertEquals(Long.valueOf(10000), apple.getShares());
        
        Holding microsoft = holdings.get(1);
        assertEquals("Microsoft Corporation", microsoft.getNameOfIssuer());
        assertEquals("594918104", microsoft.getCusip());
        assertEquals(new BigDecimal("2000000"), microsoft.getValue());
        assertEquals(Long.valueOf(20000), microsoft.getShares());
    }

    @Test
    public void testParseWithRegexFallback() {
        String xmlContent = 
            "<nameOfIssuer>Tesla Inc</nameOfIssuer>" +
            "<cusip>88160R101</cusip>" +
            "<value>500000</value>" +
            "<sshPrnamt>5000</sshPrnamt>";

        Filing filing = Enhanced13FXMLParser.parse13FContent(xmlContent, "test-456", "0001067983");
        
        assertNotNull(filing);
        List<Holding> holdings = filing.getHoldings();
        assertNotNull(holdings);
        assertEquals(1, holdings.size());
        
        Holding tesla = holdings.get(0);
        assertEquals("Tesla Inc", tesla.getNameOfIssuer());
        assertEquals("88160R101", tesla.getCusip());
        assertEquals(new BigDecimal("500000"), tesla.getValue());
        assertEquals(Long.valueOf(5000), tesla.getShares());
    }
}