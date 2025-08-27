#!/bin/bash

# SEC 13F 数据库信息查询脚本

DB_PATH="./filings.db"

echo "=============================================="
echo "     SEC 13F 数据库结构信息"
echo "=============================================="

# 检查数据库文件是否存在
if [ ! -f "$DB_PATH" ]; then
    echo "❌ 数据库文件不存在: $DB_PATH"
    exit 1
fi

echo ""
echo "📋 所有表:"
sqlite3 "$DB_PATH" ".tables"

echo ""
echo "📊 数据库统计:"
sqlite3 "$DB_PATH" "
SELECT 
    '总文件数' as 指标,
    COUNT(*) as 数值
FROM filings
UNION ALL
SELECT 
    '总持仓数' as 指标,
    COUNT(*) as 数值
FROM holdings
UNION ALL
SELECT 
    '机构数量' as 指标,
    COUNT(DISTINCT cik) as 数值
FROM filings
UNION ALL
SELECT 
    '不同证券数' as 指标,
    COUNT(DISTINCT cusip) as 数值
FROM holdings;
"

echo ""
echo "🗂 filings表结构:"
echo "列号|列名|数据类型|非空|默认值|主键"
echo "--------------------------------"
sqlite3 "$DB_PATH" "PRAGMA table_info(filings);"

echo ""
echo "📈 holdings表结构:"
echo "列号|列名|数据类型|非空|默认值|主键"
echo "--------------------------------"
sqlite3 "$DB_PATH" "PRAGMA table_info(holdings);"

echo ""
echo "🔗 外键关系:"
sqlite3 "$DB_PATH" "PRAGMA foreign_key_list(holdings);"

echo ""
echo "📝 完整表结构SQL:"
sqlite3 "$DB_PATH" ".schema"

echo ""
echo "💡 最新数据示例:"
echo "最新的3个文件:"
sqlite3 "$DB_PATH" "
SELECT 
    company_name as 机构名称,
    filing_date as 报告日期,
    accession_number as 文件编号
FROM filings 
ORDER BY filing_date DESC 
LIMIT 3;
"

echo ""
echo "前5个持仓 (按市值排序):"
sqlite3 "$DB_PATH" "
SELECT 
    h.name_of_issuer as 发行人,
    h.cusip as CUSIP,
    printf('$%,.0f', h.value) as 市值,
    printf('%,d', h.shares) as 股数
FROM holdings h
JOIN filings f ON h.filing_id = f.id
ORDER BY h.value DESC
LIMIT 5;
"

echo ""
echo "=============================================="
echo "查询完成！"
echo "=============================================="