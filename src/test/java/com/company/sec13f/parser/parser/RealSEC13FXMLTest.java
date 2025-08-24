package com.company.sec13f.parser.parser;

import com.company.sec13f.parser.model.Filing;
import com.company.sec13f.parser.model.Holding;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RealSEC13FXMLTest {

    @Test
    public void testParseRealSEC13FXML() {
        String realSECXML = "<informationTable xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.sec.gov/edgar/document/thirteenf/informationtable\">\n" +
            "  <infoTable>\n" +
            "    <nameOfIssuer>D R HORTON INC</nameOfIssuer>\n" +
            "    <titleOfClass>COM</titleOfClass>\n" +
            "    <cusip>23331A109</cusip>\n" +
            "    <value>192267725</value>\n" +
            "    <shrsOrPrnAmt>\n" +
            "      <sshPrnamt>1512371</sshPrnamt>\n" +
            "      <sshPrnamtType>SH</sshPrnamtType>\n" +
            "    </shrsOrPrnAmt>\n" +
            "    <investmentDiscretion>DFND</investmentDiscretion>\n" +
            "    <otherManager>4,11</otherManager>\n" +
            "    <votingAuthority>\n" +
            "      <Sole>1512371</Sole>\n" +
            "      <Shared>0</Shared>\n" +
            "      <None>0</None>\n" +
            "    </votingAuthority>\n" +
            "  </infoTable>\n" +
            "  <infoTable>\n" +
            "    <nameOfIssuer>LENNAR CORP</nameOfIssuer>\n" +
            "    <titleOfClass>CL A</titleOfClass>\n" +
            "    <cusip>526057104</cusip>\n" +
            "    <value>221522186</value>\n" +
            "    <shrsOrPrnAmt>\n" +
            "      <sshPrnamt>1929972</sshPrnamt>\n" +
            "      <sshPrnamtType>SH</sshPrnamtType>\n" +
            "    </shrsOrPrnAmt>\n" +
            "    <investmentDiscretion>DFND</investmentDiscretion>\n" +
            "    <otherManager>4,11</otherManager>\n" +
            "    <votingAuthority>\n" +
            "      <Sole>1929972</Sole>\n" +
            "      <Shared>0</Shared>\n" +
            "      <None>0</None>\n" +
            "    </votingAuthority>\n" +
            "  </infoTable>\n" +
            "</informationTable>";

        Filing filing = Enhanced13FXMLParser.parse13FContent(realSECXML, "0000950123-25-008361", "0001067983");
        
        assertNotNull(filing);
        assertEquals("0000950123-25-008361", filing.getAccessionNumber());
        assertEquals("0001067983", filing.getCik());
        assertEquals("13F-HR", filing.getFilingType());
        
        List<Holding> holdings = filing.getHoldings();
        assertNotNull(holdings);
        assertEquals(2, holdings.size());
        
        Holding drHorton = holdings.get(0);
        assertEquals("D R HORTON INC", drHorton.getNameOfIssuer());
        assertEquals("23331A109", drHorton.getCusip());
        assertEquals("192267725", drHorton.getValue().toString());
        assertEquals(Long.valueOf(1512371), drHorton.getShares());
        
        Holding lennar = holdings.get(1);
        assertEquals("LENNAR CORP", lennar.getNameOfIssuer());
        assertEquals("526057104", lennar.getCusip());
        assertEquals("221522186", lennar.getValue().toString());
        assertEquals(Long.valueOf(1929972), lennar.getShares());
    }
}