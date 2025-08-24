#!/bin/bash

# Alibaba 13F Parser Runner Script

echo "==========================================="
echo "阿里巴巴控股集团 SEC 13F 持仓分析器"
echo "==========================================="
echo

# Check if Maven is installed
if ! command -v mvn &> /dev/null
then
    echo "错误: 未找到Maven，请先安装Maven"
    exit 1
fi

echo "正在编译项目..."
mvn compile

if [ $? -ne 0 ]; then
    echo "编译失败"
    exit 1
fi

echo
echo "正在运行阿里巴巴13F分析器..."
echo "==========================================="
mvn exec:java -Dexec.mainClass="com.company.sec13f.parser.Alibaba13FExample"

if [ $? -eq 0 ]; then
    echo
    echo "==========================================="
    echo "分析完成！"
    echo "生成的HTML报告文件: alibaba_13f_holdings.html"
    echo "请在浏览器中打开此文件查看详细持仓信息"
    echo "==========================================="
else
    echo "运行失败"
    exit 1
fi
