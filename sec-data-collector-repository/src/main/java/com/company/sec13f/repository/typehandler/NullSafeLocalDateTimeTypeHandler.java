package com.company.sec13f.repository.typehandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.*;
import java.time.LocalDateTime;

/**
 * NULL安全的LocalDateTime类型处理器
 * 用于处理MySQL中的TIMESTAMP字段，支持NULL值
 */
@MappedTypes(LocalDateTime.class)
@MappedJdbcTypes({JdbcType.TIMESTAMP, JdbcType.VARCHAR})
public class NullSafeLocalDateTimeTypeHandler extends BaseTypeHandler<LocalDateTime> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, LocalDateTime parameter, JdbcType jdbcType) throws SQLException {
        if (parameter == null) {
            ps.setNull(i, Types.TIMESTAMP);
        } else {
            ps.setTimestamp(i, Timestamp.valueOf(parameter));
        }
    }

    @Override
    public LocalDateTime getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseDateTime(rs.getString(columnName));
    }

    @Override
    public LocalDateTime getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseDateTime(rs.getString(columnIndex));
    }

    @Override
    public LocalDateTime getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseDateTime(cs.getString(columnIndex));
    }
    
    /**
     * 解析多种时间戳格式，兼容MySQL数据库
     */
    private LocalDateTime parseDateTime(String value) throws SQLException {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        
        try {
            // 尝试解析ISO格式：2025-08-29T01:59:43.044272
            if (value.contains("T")) {
                // 去掉纳秒部分，只保留到毫秒
                if (value.contains(".")) {
                    String[] parts = value.split("\\.");
                    if (parts.length > 1 && parts[1].length() > 3) {
                        value = parts[0] + "." + parts[1].substring(0, 3);
                    }
                }
                return LocalDateTime.parse(value);
            }
            
            // 尝试解析简化格式：2025-08-28 17:59:42
            if (value.contains(" ")) {
                return LocalDateTime.parse(value.replace(" ", "T"));
            }
            
            // 尝试解析时间戳格式
            if (value.matches("\\d+")) {
                long timestamp = Long.parseLong(value);
                return LocalDateTime.ofEpochSecond(timestamp / 1000, 
                    (int) ((timestamp % 1000) * 1000000), 
                    java.time.ZoneOffset.UTC);
            }
            
            return LocalDateTime.parse(value);
            
        } catch (Exception e) {
            throw new SQLException("Cannot parse datetime value: " + value, e);
        }
    }
}