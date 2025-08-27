#!/bin/bash

# SEC 13F Parser 日志查看工具

LOG_FILE="./sec13f-parser.log"

echo "=============================================="
echo "     SEC 13F Parser 日志查看工具"
echo "=============================================="

# 检查日志文件是否存在
if [ ! -f "$LOG_FILE" ]; then
    echo "❌ 日志文件不存在: $LOG_FILE"
    echo "💡 提示: 启动系统后会自动创建日志文件"
    exit 1
fi

# 显示选项菜单
echo ""
echo "请选择查看方式:"
echo "1. 查看最新50行日志"
echo "2. 实时监控日志 (tail -f)"
echo "3. 查看所有日志"
echo "4. 查看错误日志"
echo "5. 查看爬取相关日志"
echo "6. 搜索日志内容"
echo "7. 日志统计信息"
echo "0. 退出"
echo ""

read -p "请输入选项 (0-7): " choice

case $choice in
    1)
        echo ""
        echo "📄 最新50行日志:"
        echo "----------------------------------------"
        tail -n 50 "$LOG_FILE"
        ;;
    2)
        echo ""
        echo "🔄 实时监控日志 (按 Ctrl+C 退出):"
        echo "----------------------------------------"
        tail -f "$LOG_FILE"
        ;;
    3)
        echo ""
        echo "📋 所有日志内容:"
        echo "----------------------------------------"
        cat "$LOG_FILE"
        ;;
    4)
        echo ""
        echo "❌ 错误日志:"
        echo "----------------------------------------"
        grep "ERROR" "$LOG_FILE" || echo "没有找到错误日志"
        ;;
    5)
        echo ""
        echo "🕷️ 爬取相关日志:"
        echo "----------------------------------------"
        grep -E "(SCRAPING|SEC_REQUEST)" "$LOG_FILE" || echo "没有找到爬取相关日志"
        ;;
    6)
        read -p "请输入搜索关键词: " keyword
        echo ""
        echo "🔍 搜索结果 (关键词: $keyword):"
        echo "----------------------------------------"
        grep -i "$keyword" "$LOG_FILE" || echo "没有找到匹配的日志"
        ;;
    7)
        echo ""
        echo "📊 日志统计信息:"
        echo "----------------------------------------"
        echo "总行数: $(wc -l < "$LOG_FILE")"
        echo "INFO 日志: $(grep -c "INFO" "$LOG_FILE")"
        echo "WARN 日志: $(grep -c "WARN" "$LOG_FILE")"
        echo "ERROR 日志: $(grep -c "ERROR" "$LOG_FILE")"
        echo "DEBUG 日志: $(grep -c "DEBUG" "$LOG_FILE")"
        echo ""
        echo "爬取开始: $(grep -c "SCRAPING_STARTED" "$LOG_FILE")"
        echo "爬取完成: $(grep -c "SCRAPING_COMPLETED" "$LOG_FILE")"
        echo "爬取失败: $(grep -c "SCRAPING_FAILED" "$LOG_FILE")"
        echo ""
        echo "文件大小: $(du -h "$LOG_FILE" | cut -f1)"
        echo "最后修改: $(stat -f "%Sm" -t "%Y-%m-%d %H:%M:%S" "$LOG_FILE")"
        ;;
    0)
        echo "退出日志查看工具"
        exit 0
        ;;
    *)
        echo "❌ 无效选项，请重新运行脚本"
        exit 1
        ;;
esac

echo ""
echo "=============================================="