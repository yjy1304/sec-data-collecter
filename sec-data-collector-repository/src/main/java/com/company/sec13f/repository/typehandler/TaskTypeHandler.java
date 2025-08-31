package com.company.sec13f.repository.typehandler;

import com.company.sec13f.repository.enums.TaskType;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 自定义TaskType枚举类型处理器
 * 确保正确处理task_type列的映射
 */
public class TaskTypeHandler extends BaseTypeHandler<TaskType> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, TaskType parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.name());
    }

    @Override
    public TaskType getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return value == null ? null : TaskType.valueOf(value);
    }

    @Override
    public TaskType getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return value == null ? null : TaskType.valueOf(value);
    }

    @Override
    public TaskType getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return value == null ? null : TaskType.valueOf(value);
    }
}