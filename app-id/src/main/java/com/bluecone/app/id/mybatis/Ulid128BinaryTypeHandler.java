package com.bluecone.app.id.mybatis;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import com.bluecone.app.id.core.Ulid128;

/**
 * Ulid128 对应 MySQL BINARY(16) 字段的 TypeHandler。
 */
@MappedTypes(Ulid128.class)
@MappedJdbcTypes({JdbcType.BINARY, JdbcType.VARBINARY})
public class Ulid128BinaryTypeHandler extends BaseTypeHandler<Ulid128> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Ulid128 parameter, JdbcType jdbcType)
            throws SQLException {
        byte[] bytes = parameter.toBytes();
        ps.setBytes(i, bytes);
    }

    @Override
    public Ulid128 getNullableResult(ResultSet rs, String columnName) throws SQLException {
        byte[] bytes = rs.getBytes(columnName);
        return convertBytes(bytes);
    }

    @Override
    public Ulid128 getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        byte[] bytes = rs.getBytes(columnIndex);
        return convertBytes(bytes);
    }

    @Override
    public Ulid128 getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        byte[] bytes = cs.getBytes(columnIndex);
        return convertBytes(bytes);
    }

    private Ulid128 convertBytes(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        if (bytes.length != 16) {
            throw new IllegalArgumentException("BINARY(16) 字段读取到长度=" + bytes.length + "，期望=16");
        }
        return Ulid128.fromBytes(bytes);
    }
}

