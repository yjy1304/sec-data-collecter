#!/bin/bash

# 更新所有Java文件中的import语句
find sec-data-collector-*/ -name "*.java" -exec sed -i '' \
    -e 's/import com\.company\.sec13f\.parser\.web\./import com.company.sec13f.web./g' \
    -e 's/import com\.company\.sec13f\.parser\.service\./import com.company.sec13f.service./g' \
    -e 's/import com\.company\.sec13f\.parser\.scraper\./import com.company.sec13f.service.scraper./g' \
    -e 's/import com\.company\.sec13f\.parser\.parser\./import com.company.sec13f.service.parser./g' \
    -e 's/import com\.company\.sec13f\.parser\.util\./import com.company.sec13f.service.util./g' \
    -e 's/import com\.company\.sec13f\.parser\.config\./import com.company.sec13f.service.config./g' \
    -e 's/import com\.company\.sec13f\.parser\.database\./import com.company.sec13f.repository.database./g' \
    -e 's/import com\.company\.sec13f\.parser\.model\./import com.company.sec13f.repository.model./g' \
    -e 's/import com\.company\.sec13f\.repository\.entity\./import com.company.sec13f.repository.entity./g' \
    -e 's/import com\.company\.sec13f\.repository\.mapper\./import com.company.sec13f.repository.mapper./g' \
    -e 's/import com\.company\.sec13f\.repository\.service\./import com.company.sec13f.repository.service./g' \
    {} \;

echo "Import statements updated successfully"