#!/bin/bash

# 修复service模块中的类型引用问题
cd sec-data-collector-service

# 添加import语句
sed -i '' '5a\
import com.company.sec13f.repository.model.ScrapingTask;\
import com.company.sec13f.repository.enums.TaskStatus;
' src/main/java/com/company/sec13f/service/DataScrapingService.java

# 替换所有ScrapingStatus为ScrapingTask
find . -name "*.java" -exec sed -i '' \
    -e 's/ScrapingStatus/ScrapingTask/g' \
    -e 's/private final Map<String, ScrapingTask> scrapingTasks/private final Map<String, ScrapingTask> scrapingTasks/g' \
    {} \;

echo "Service types fixed"