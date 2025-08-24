package com.company.sec13f.parser.integration;

import com.company.sec13f.parser.database.FilingDAO;
import com.company.sec13f.parser.model.Filing;
import com.company.sec13f.parser.model.Holding;
import com.company.sec13f.parser.parser.Enhanced13FXMLParser;
import com.company.sec13f.parser.util.DataValidator;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class Complete13FProcessingTest {

    @Test
    public void testComplete13FProcessingWithRealData() {
        // Real XML content from Berkshire Hathaway 13F filing
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

        String testAccessionNumber = "0001067983-25-999999"; // Valid SEC format
        String testCik = "0001067983";

        try {
            // Step 1: Parse XML using Enhanced13FXMLParser
            Filing filing = Enhanced13FXMLParser.parse13FContent(realSECXML, testAccessionNumber, testCik);
            filing.setCompanyName("Test Berkshire Hathaway Inc");
            filing.setFilingDate(java.time.LocalDate.of(2025, 8, 24));
            
            assertNotNull(filing, "Filing should not be null");
            assertEquals(testAccessionNumber, filing.getAccessionNumber());
            assertEquals(testCik, filing.getCik());
            assertEquals("13F-HR", filing.getFilingType());
            
            List<Holding> holdings = filing.getHoldings();
            assertNotNull(holdings, "Holdings should not be null");
            assertEquals(2, holdings.size(), "Should have 2 holdings");
            
            // Step 2: Validate the filing
            DataValidator.ValidationResult validation = DataValidator.validateFiling(filing);
            assertTrue(validation.isValid(), "Filing should be valid: " + (validation.getAllErrors().size() > 0 ? validation.getAllErrors().get(0) : ""));
            
            // Step 3: Save to database
            FilingDAO filingDAO = new FilingDAO();
            filingDAO.saveFiling(filing);
            
            // Step 4: Verify database storage
            Long filingId = filingDAO.getFilingIdByAccessionNumber(testAccessionNumber);
            assertNotNull(filingId, "Filing should be saved to database");
            
            System.out.println("✅ Complete 13F processing test PASSED!");
            System.out.println("   - Parsed 2 holdings from real SEC XML format");
            System.out.println("   - Validated filing successfully");
            System.out.println("   - Saved filing to database with ID: " + filingId);
            System.out.println("   - Filing: " + testAccessionNumber + " for " + filing.getCompanyName());
            
        } catch (Exception e) {
            fail("Complete 13F processing failed: " + e.getMessage());
        }
    }
}