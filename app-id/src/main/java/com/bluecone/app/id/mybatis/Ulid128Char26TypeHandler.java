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

import de.huxhorn.sulky.ulid.ULID;

/**
 * Ulid128 对应 CHAR(26)/VARCHAR(26) 字段的 TypeHandler。
 *
 * <p>用于过渡期存储 ULID 字符串形式。</p>
 */
@MappedTypes(Ulid128.class)
@MappedJdbcTypes({JdbcType.CHAR, JdbcType.VARCHAR})
public class Ulid128Char26TypeHandler extends BaseTypeHandler<Ulid128> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Ulid128 parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, parameter.toString());
    }

    @Override
    public Ulid128 getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String s = rs.getString(columnName);
        return parseString(s);
    }

    @Override
    public Ulid128 getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String s = rs.getString(columnIndex);
        return parseString(s);
    }

    @Override
    public Ulid128 getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String s = cs.getString(columnIndex);
        return parseString(s);
    }

    private Ulid128 parseString(String s) {
        if (s == null) {
            return null;
        }
        String trimmed = s.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() != 26) {
            throw new IllegalArgumentException("CHAR(26) 字段读取到长度=" + trimmed.length() + "，期望=26");
        }
        try {
            ULID.Value value = ULID.parseULID(trimmed);
            return new Ulid128(value.getMostSignificantBits(), value.getLeastSignificantBits());
        } catch (IllegalArgumentException ex) {
            String safe = trimmed;
            if (safe.length() > 32) {
                safe = safe.substring(0, 32) + "...";
            }
            throw new IllegalArgumentException("ULID 字符串解析失败，输入为: " + safe, ex);
        }
    }
}

