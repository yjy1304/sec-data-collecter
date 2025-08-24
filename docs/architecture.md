# 项目概述

  这是一个SEC 13F文件解析和机构持仓分析系统，用于从美国证券交易委员会(SEC)
  获取、解析和分析机构投资者的持仓信息。

  ## 技术架构

  - 语言: Java 8
  - 构建工具: Maven
  - Web服务器: Jetty (嵌入式)
  - 数据库: SQLite
  - 前端: HTML/CSS/JavaScript

  ## 主要功能模块

  1. 数据爬取模块 (scraper/)

  - RealSECScraper.java: 真实SEC数据爬取引擎
  - EnhancedSECScraper.java: 增强版爬取器
  - SECScraper.java: 基础爬取器
  - DataScrapingService.java: 爬取任务管理服务

  2. 数据解析模块 (parser/)

  - SEC13FXMLParser.java: SEC 13F XML文件解析器
  - FilingParser.java: 通用文件解析器

  3. 数据库模块 (database/)

  - DatabaseInitializer.java: 数据库初始化
  - FilingDAO.java: 数据访问对象
  - FilingDatabaseService.java: 数据库服务层

  4. 数据模型 (model/)

  - Filing.java: 文件实体模型
  - Holding.java: 持仓记录模型

  5. 业务服务层 (service/)

  - HoldingAnalysisService.java: 持仓分析核心服务
  - FilingService.java: 文件服务
  - DataScrapingService.java: 数据爬取服务

  6. Web接口层 (web/)

  - AnalysisServlet.java: 分析API接口
  - ScrapingServlet.java: 爬取管理API
  - SearchServlet.java: 搜索接口
  - FilingsServlet.java: 文件查询接口
  - HomeServlet.java: 主页接口

  7. 报告生成 (report/)

  - ReportGenerator.java: 报告生成器接口
  - SimpleHtmlReportGenerator.java: HTML报告生成器

  8. 工具类 (util/)

  - DataValidator.java: 数据验证工具
  - Logger.java: 日志工具
  - Utils.java: 通用工具

  9. Web服务器

  - WebServer.java: 嵌入式Jetty服务器主类

  ## 核心功能特点

  1. 真实数据获取: 从SEC EDGAR官方API获取实时13F文件
  2. 智能解析: 支持XML格式的13F文件解析
  3. 数据分析: 提供持仓分析、趋势分析、投资组合摘要
  4. Web界面: 现代化的用户界面进行数据查询和可视化
  5. 任务管理: 可视化的爬取任务管理和进度监控

  ## 主要依赖

  - Apache HttpClient (网络请求)
  - Jackson/Gson (JSON处理)
  - SQLite JDBC (数据库)
  - Jetty (Web服务器)
  - Apache POI (Excel导出)
  - JAXB (XML解析)

  项目采用分层架构设计，职责清晰，支持从SEC官方获取真实的机构持仓数据并进行
  深度分析。