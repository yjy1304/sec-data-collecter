package com.company.sec13f.parser.parser;

import com.company.sec13f.parser.model.Filing;
import com.company.sec13f.parser.model.Holding;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Parser for SEC 13F XML filings
 */
public class SEC13FXMLParser {
    
    /**
     * Parses an SEC 13F XML filing into a Filing object
     *
     * @param xmlContent The XML content of the 13F filing
     * @param cik The CIK number of the filer
     * @param companyName The name of the filer
     * @return A Filing object representing the parsed data
     * @throws Exception If an error occurs during parsing
     */
    public Filing parseFiling(String xmlContent, String cik, String companyName) throws Exception {
        if (xmlContent == null || xmlContent.isEmpty()) {
            throw new IllegalArgumentException("XML content cannot be null or empty");
        }
        
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xmlContent)));
            
            Filing filing = new Filing();
            filing.setCik(cik);
            filing.setCompanyName(companyName);
            filing.setFilingType("13F-HR");
            
            // Parse the filing date from the XML
            NodeList filingDates = document.getElementsByTagName("filingDate");
            if (filingDates.getLength() > 0) {
                String dateString = filingDates.item(0).getTextContent();
                try {
                    LocalDate filingDate = LocalDate.parse(dateString, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    filing.setFilingDate(filingDate);
                } catch (Exception e) {
                    System.err.println("Warning: Could not parse filing date: " + dateString);
                }
            }
            
            // Parse the accession number from the XML
            NodeList accessionNumbers = document.getElementsByTagName("accessionNumber");
            if (accessionNumbers.getLength() > 0) {
                filing.setAccessionNumber(accessionNumbers.item(0).getTextContent());
            }
            
            // Parse the holdings
            List<Holding> holdings = parseHoldings(document);
            filing.setHoldings(holdings);
            
            return filing;
        } catch (ParserConfigurationException e) {
            throw new Exception("Could not configure XML parser", e);
        } catch (Exception e) {
            throw new Exception("Error parsing XML content", e);
        }
    }
    
    /**
     * Parses the holdings from the XML document
     *
     * @param document The XML document
     * @return A list of Holding objects
     * @throws Exception If an error occurs during parsing
     */
    private List<Holding> parseHoldings(Document document) throws Exception {
        List<Holding> holdings = new ArrayList<>();
        
        NodeList infoTables = document.getElementsByTagName("infoTable");
        for (int i = 0; i < infoTables.getLength(); i++) {
            Element infoTable = (Element) infoTables.item(i);
            Holding holding = holding(infoTable);
            if (holding != null) {
                holdings.add(holding);
            }
        }
        
        return holdings;
    }
    
    /**
     * Parses a single holding from an infoTable element
     *
     * @param infoTable The infoTable element
     * @return A Holding object, or null if parsing fails
     */
    private Holding holding(Element infoTable) {
        try {
            Holding holding = new Holding();
            
            // Parse nameOfIssuer
            NodeList nameOfIssuerNodes = infoTable.getElementsByTagName("nameOfIssuer");
            if (nameOfIssuerNodes.getLength() > 0) {
                holding.setNameOfIssuer(nameOfIssuerNodes.item(0).getTextContent().trim());
            }
            
            // Parse cusip
            NodeList cusipNodes = infoTable.getElementsByTagName("cusip");
            if (cusipNodes.getLength() > 0) {
                holding.setCusip(cusipNodes.item(0).getTextContent().trim());
            }
            
            // Parse value
            NodeList valueNodes = infoTable.getElementsByTagName("value");
            if (valueNodes.getLength() > 0) {
                String valueStr = valueNodes.item(0).getTextContent().trim();
                try {
                    BigDecimal value = new BigDecimal(valueStr);
                    holding.setValue(value);
                } catch (NumberFormatException e) {
                    System.err.println("Warning: Could not parse value: " + valueStr);
                }
            }
            
            // Parse shares
            NodeList sharesNodes = infoTable.getElementsByTagName("sshPrnamt");
            if (sharesNodes.getLength() > 0) {
                String sharesStr = sharesNodes.item(0).getTextContent().trim();
                try {
                    Long shares = Long.parseLong(sharesStr);
                    holding.setShares(shares);
                } catch (NumberFormatException e) {
                    System.err.println("Warning: Could not parse shares: " + sharesStr);
                }
            }
            
            return holding;
        } catch (Exception e) {
            System.err.println("Warning: Could not parse holding: " + e.getMessage());
            return null;
        }
    }
}