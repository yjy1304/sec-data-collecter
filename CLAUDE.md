# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a comprehensive SEC 13F filing parser and analysis system that scrapes real data from the SEC EDGAR database, stores it in a local SQLite database, and provides a web-based interface for querying and analyzing institutional investment holdings.

### Key Features
- Real-time data scraping from SEC EDGAR official API (data.sec.gov)
- SQLite database storage with MyBatis ORM framework
- Multi-module Maven architecture with Spring Boot
- Web-based analysis interface with modern HTML/CSS/JS frontend
- Batch processing and task management system
- Investment portfolio analysis and trend tracking
- Export capabilities for various formats

## Architecture Overview

### Module Structure
The project follows a **multi-module Maven architecture** with Spring Boot:

```
sec-data-collector/                # Parent module (POM)
├── sec-data-collector-repository  # Data access layer (JAR)
├── sec-data-collector-service     # Business logic layer (JAR)  
└── sec-data-collector-web         # Web interface layer (JAR, executable)

src/main/java/com/company/sec13f/parser/  # Legacy monolith (being migrated)
├── web/                           # Legacy servlet-based web layer
├── service/                       # Legacy business logic services
├── scraper/                       # SEC data scraping engines
├── parser/                        # XML/data parsing utilities
├── database/                      # Legacy database layer (deprecated)
└── model/                         # Legacy domain models
```

### Technology Stack

**Backend:**
- Java 8 (minimum requirement)
- Maven 3.6+ for build management  
- **Spring Boot 2.7.12** (NEW - primary framework)
- **Spring MVC** for REST APIs (NEW - replacing Servlets)
- Jetty 9.4.51 embedded web server (LEGACY)
- SQLite 3.42.0 database
- MyBatis 3.5.13 ORM framework
- Apache HttpClient 4.5.13 for web scraping
- Jackson 2.15.2 for JSON processing
- Spring 5.3.27 (Context, TX, AOP, Retry)
- JAXB 2.3.1 for XML parsing

**Frontend:**
- Modern HTML5/CSS3/JavaScript  
- Responsive design for multiple screen sizes
- AJAX-based API communication
- Real-time task progress monitoring

**Database:**
- SQLite for local storage
- MyBatis for type-safe database operations
- Connection pooling and transaction management

## Database Schema

### Core Tables

**filings** - SEC 13F filing metadata
```sql
CREATE TABLE filings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    cik TEXT NOT NULL,                    -- SEC Central Index Key
    company_name TEXT NOT NULL,           -- Institution name
    filing_type TEXT NOT NULL,            -- Always "13F-HR"
    filing_date DATE NOT NULL,            -- Reporting date
    accession_number TEXT NOT NULL,       -- SEC accession number
    form_file TEXT NOT NULL,              -- Original file name
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(accession_number, form_file)
);
```

**holdings** - Individual stock positions
```sql
CREATE TABLE holdings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    filing_id INTEGER NOT NULL,           -- Reference to filings.id
    name_of_issuer TEXT NOT NULL,         -- Company name (e.g., "Apple Inc")
    cusip TEXT NOT NULL,                  -- CUSIP identifier
    value DECIMAL(15,2),                  -- Market value in thousands USD
    shares BIGINT,                        -- Number of shares held
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (filing_id) REFERENCES filings(id) ON DELETE CASCADE
);
```

**scraping_tasks** - Task management and monitoring
```sql
CREATE TABLE scraping_tasks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id TEXT NOT NULL UNIQUE,         -- Unique task identifier
    cik TEXT NOT NULL,                    -- Target institution CIK
    company_name TEXT NOT NULL,           -- Institution name
    status TEXT NOT NULL,                 -- PENDING/RUNNING/COMPLETED/FAILED
    message TEXT,                         -- Status message
    error_message TEXT,                   -- Error details if failed
    start_time TIMESTAMP,                 -- Task start time
    end_time TIMESTAMP,                   -- Task completion time
    saved_filings INTEGER DEFAULT 0,      -- Number of filings processed
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## Key Components

### 1. Data Persistence Layer (NEW - MyBatis-based)

**Location:** `sec-data-collector-repository/src/main/java/com/company/sec13f/repository/`

**Entity Classes:**
- `Filing.java` - Enhanced filing entity with audit fields and relationship mapping
- `Holding.java` - Individual stock holding entity
- `ScrapingTask.java` - Task entity with enum status and duration calculation

**Mapper Interfaces:**
- `FilingMapper.java` - Type-safe CRUD operations for filings
- `HoldingMapper.java` - Holdings-specific database operations  
- `ScrapingTaskMapper.java` - Task management operations

**Service Layer:**
- `FilingRepositoryService.java` - Transaction-managed filing operations with batch processing
- `ScrapingTaskRepositoryService.java` - Task lifecycle management

**Key Features:**
- Automatic transaction management
- Batch operations for performance
- Comprehensive logging with emoji indicators
- Type-safe enum handling
- Audit trail support (created_at, updated_at)

### 2. Web Server & API Layer

**Main Entry Point:** `WebServer.java`
- Jetty embedded server on port 8080
- Servlet-based request handling
- Spring integration for dependency injection
- Static resource serving for frontend

**Key Servlets:**
- `AnalysisServlet.java` - Portfolio analysis APIs (`/api/analysis/*`)
- `ScrapingServlet.java` - Data collection APIs (`/api/scraping/*`)
- `SearchServlet.java` - Basic search functionality (`/search`)
- `TaskManagementServlet.java` - Task monitoring (`/tasks/*`)
- `SchedulingServlet.java` - Automated scheduling (`/api/scheduling/*`)

### 3. Data Scraping Engine

**Core Components:**
- `RealSECScraper.java` - Official SEC EDGAR API client
- `DataScrapingService.java` - Batch processing and task management
- `EnhancedSECScraper.java` - Advanced scraping with retry logic

**Features:**
- Rate limiting (100ms intervals) to comply with SEC terms
- Automatic retry with exponential backoff
- Data validation and cleaning
- Progress tracking and logging

### 4. Analysis Services

**HoldingAnalysisService.java** - Core business logic:
- Institution overview analysis
- Top holdings ranking by market value
- Portfolio summary with sector allocation
- Holding change analysis between periods
- Historical trend analysis for specific securities

### 5. Frontend Interface

**Web Pages:**
- `index.html` - Main landing page with basic search
- `analysis.html` - Advanced portfolio analysis interface
- `scraping.html` - Data collection management
- `database.html` - Database statistics and management

**Features:**
- Responsive design for desktop and mobile
- Real-time progress monitoring
- Interactive charts and tables
- AJAX-based API communication

## Build and Deployment

### Development Setup

**Multi-module Spring Boot build (NEW):**
```bash
# 1. Build all modules from root
mvn clean compile

# 2. Package the web application
mvn package -DskipTests

# 3. Run the Spring Boot application
cd sec-data-collector-web
mvn spring-boot:run

# OR run the JAR directly
java -jar sec-data-collector-web/target/sec-data-collector-web-1.0.0.jar
```

**Legacy monolith build (for compatibility):**
```bash
# Build legacy WebServer for backward compatibility
mvn clean compile exec:java -Dexec.mainClass="com.company.sec13f.parser.WebServer"
```

### Testing

**Repository Module Testing:**
```bash
cd sec-data-collector-repository
mvn exec:java -Dexec.mainClass="com.company.sec13f.repository.test.RepositoryTest"
```

**Unit Tests:**
```bash
mvn test                           # Run all tests
mvn test -Dtest=ClassName         # Run specific test class
```

**Integration Testing:**
- `Complete13FProcessingTest.java` - End-to-end processing tests  
- `Enhanced13FXMLParserTest.java` - XML parsing validation
- `SECScraperTest.java` - Data scraping functionality tests

### Application URLs

- **Main Interface:** http://localhost:8080
- **Spring Boot Actuator:** http://localhost:8080/actuator/health
- **Advanced Analysis:** http://localhost:8080/analysis.html
- **Data Scraping:** http://localhost:8080/scraping.html
- **Database Management:** http://localhost:8080/database.html

## API Reference

### Analysis APIs

**Institution Overview:**
```
GET /api/analysis/overview?cik=0001524258
```

**Top Holdings:**
```
GET /api/analysis/top-holdings?cik=0001524258&limit=20
```

**Portfolio Summary:**
```
GET /api/analysis/portfolio-summary?cik=0001524258
```

**Holding Changes:**
```
GET /api/analysis/holding-changes?cik=0001524258
```

### Scraping APIs

**Single Institution:**
```
POST /api/scraping/scrape
Body: cik=0001524258&companyName=Alibaba Group Holding Limited
```

**Batch Processing:**
```
POST /api/scraping/scrape-batch
```

**Task Status:**
```
GET /api/scraping/status?taskId=scrape_0001524258_1692308734567
GET /api/scraping/tasks
```

## Development Guidelines

### Code Style
- Follow existing patterns in the codebase
- Use comprehensive logging with emoji indicators for readability
- Implement proper error handling and validation
- Maintain backward compatibility when possible

### Database Operations
- **NEW CODE:** Use MyBatis repository layer in `sec-data-collector-repository`
- **LEGACY CODE:** Legacy DAO classes in main module are being phased out
- Always use transactions for multi-table operations
- Implement proper connection pooling

### Performance Considerations
- Batch operations for large datasets
- Connection pooling for database access  
- Rate limiting for SEC API requests (100ms minimum intervals)
- Caching for frequently accessed data

### Legal Compliance
- Respect SEC website terms of service
- Implement request rate limiting
- Ensure data accuracy through validation
- Include proper attribution in reports

## Common Issues and Solutions

### Database Issues
- **Connection errors:** Check SQLite file permissions
- **Schema errors:** Run `MyBatisSessionFactory.initializeTables()`
- **Transaction issues:** Ensure proper session management

### Scraping Issues  
- **Rate limiting:** SEC enforces request frequency limits
- **Network timeouts:** Implement proper retry logic
- **Data validation:** Use DataValidator for data cleaning

### Build Issues
- **Module dependencies:** Build repository module first with `mvn install`
- **Missing dependencies:** Check all Maven dependencies are available
- **Classpath issues:** Verify Maven Shade plugin configuration

## Future Development

### Planned Enhancements
- Migration of legacy database layer to MyBatis
- Enhanced caching mechanisms
- User authentication and authorization
- Additional report formats (PDF, Excel)
- Database clustering for production deployment

### Architecture Improvements
- Complete migration to repository pattern
- Microservices architecture consideration
- Enhanced monitoring and metrics
- Cloud deployment capabilities

## Data Sources

**SEC EDGAR Official API**
- **Endpoint:** https://data.sec.gov
- **Authentication:** None required
- **Rate Limits:** Respect official guidelines (100ms intervals)
- **Data Coverage:** 2013-present, quarterly updates

**Sample Institutions:**
- Berkshire Hathaway (CIK: 0001067983)
- Alibaba Group (CIK: 0001524258) 
- BlackRock, Vanguard, State Street, and hundreds more

---

**Note:** This project is for educational and research purposes. Users must comply with SEC website terms of service. All data comes from publicly available SEC EDGAR database.