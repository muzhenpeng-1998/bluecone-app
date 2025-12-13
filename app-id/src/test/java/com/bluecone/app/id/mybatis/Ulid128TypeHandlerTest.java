package com.bluecone.app.id.mybatis;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.Test;

import com.bluecone.app.id.core.Ulid128;

/**
 * Ulid128 MyBatis TypeHandler 行为测试。
 */
class Ulid128TypeHandlerTest {

    @Test
    void binaryHandlerSetAndGetShouldBeReversible() throws SQLException {
        Ulid128BinaryTypeHandler handler = new Ulid128BinaryTypeHandler();
        Ulid128 ulid = new Ulid128(0x1234_5678_9ABCDEFL, 0x0FED_CBA9_8765_4321L);

        final byte[][] captured = new byte[1][];
        PreparedStatement ps = (PreparedStatement) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{PreparedStatement.class},
                (proxy, method, args) -> {
                    if ("setBytes".equals(method.getName())) {
                        captured[0] = (byte[]) args[1];
                        return null;
                    }
                    Class<?> rt = method.getReturnType();
                    if (rt.equals(boolean.class)) {
                        return false;
                    } else if (rt.equals(int.class)) {
                        return 0;
                    } else if (rt.equals(long.class)) {
                        return 0L;
                    }
                    return null;
                });
        handler.setNonNullParameter(ps, 1, ulid, JdbcType.BINARY);

        byte[] bytes = captured[0];
        assertNotNull(bytes);
        assertEquals(16, bytes.length);
        assertArrayEquals(ulid.toBytes(), bytes);

        ResultSet rs = (ResultSet) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{ResultSet.class},
                (proxy, method, args) -> {
                    if ("getBytes".equals(method.getName())) {
                        return bytes;
                    }
                    Class<?> rt = method.getReturnType();
                    if (rt.equals(boolean.class)) {
                        return false;
                    } else if (rt.equals(int.class)) {
                        return 0;
                    } else if (rt.equals(long.class)) {
                        return 0L;
                    }
                    return null;
                });
        Ulid128 restored = handler.getNullableResult(rs, "id");
        assertNotNull(restored);
        assertEquals(ulid.msb(), restored.msb());
        assertEquals(ulid.lsb(), restored.lsb());
    }

    @Test
    void binaryHandlerInvalidLengthShouldThrow() throws SQLException {
        Ulid128BinaryTypeHandler handler = new Ulid128BinaryTypeHandler();
        ResultSet rs = (ResultSet) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{ResultSet.class},
                (proxy, method, args) -> {
                    if ("getBytes".equals(method.getName())) {
                        return new byte[15];
                    }
                    Class<?> rt = method.getReturnType();
                    if (rt.equals(boolean.class)) {
                        return false;
                    } else if (rt.equals(int.class)) {
                        return 0;
                    } else if (rt.equals(long.class)) {
                        return 0L;
                    }
                    return null;
                });

        assertThrows(IllegalArgumentException.class, () -> handler.getNullableResult(rs, 1));

        CallableStatement cs = (CallableStatement) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{CallableStatement.class},
                (proxy, method, args) -> {
                    if ("getBytes".equals(method.getName())) {
                        return new byte[17];
                    }
                    Class<?> rt = method.getReturnType();
                    if (rt.equals(boolean.class)) {
                        return false;
                    } else if (rt.equals(int.class)) {
                        return 0;
                    } else if (rt.equals(long.class)) {
                        return 0L;
                    }
                    return null;
                });
        assertThrows(IllegalArgumentException.class, () -> handler.getNullableResult(cs, 1));
    }

    @Test
    void charHandlerSetAndGetShouldBeReversible() throws SQLException {
        Ulid128Char26TypeHandler handler = new Ulid128Char26TypeHandler();
        Ulid128 ulid = new Ulid128(0x1234_5678_9ABCDEFL, 0x0FED_CBA9_8765_4321L);

        final String[] captured = new String[1];
        PreparedStatement ps = (PreparedStatement) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{PreparedStatement.class},
                (proxy, method, args) -> {
                    if ("setString".equals(method.getName())) {
                        captured[0] = (String) args[1];
                        return null;
                    }
                    Class<?> rt = method.getReturnType();
                    if (rt.equals(boolean.class)) {
                        return false;
                    } else if (rt.equals(int.class)) {
                        return 0;
                    } else if (rt.equals(long.class)) {
                        return 0L;
                    }
                    return null;
                });
        handler.setNonNullParameter(ps, 1, ulid, JdbcType.CHAR);

        String stored = captured[0];
        assertNotNull(stored);
        assertEquals(26, stored.length());

        ResultSet rs = (ResultSet) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{ResultSet.class},
                (proxy, method, args) -> {
                    if ("getString".equals(method.getName())) {
                        return stored;
                    }
                    Class<?> rt = method.getReturnType();
                    if (rt.equals(boolean.class)) {
                        return false;
                    } else if (rt.equals(int.class)) {
                        return 0;
                    } else if (rt.equals(long.class)) {
                        return 0L;
                    }
                    return null;
                });
        Ulid128 restored = handler.getNullableResult(rs, "id");
        assertNotNull(restored);
        assertEquals(ulid.msb(), restored.msb());
        assertEquals(ulid.lsb(), restored.lsb());
    }

    @Test
    void charHandlerInvalidStringShouldThrow() throws SQLException {
        Ulid128Char26TypeHandler handler = new Ulid128Char26TypeHandler();
        ResultSet rs = (ResultSet) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{ResultSet.class},
                (proxy, method, args) -> {
                    if ("getString".equals(method.getName())) {
                        return "abc";
                    }
                    Class<?> rt = method.getReturnType();
                    if (rt.equals(boolean.class)) {
                        return false;
                    } else if (rt.equals(int.class)) {
                        return 0;
                    } else if (rt.equals(long.class)) {
                        return 0L;
                    }
                    return null;
                });

        assertThrows(IllegalArgumentException.class, () -> handler.getNullableResult(rs, "id"));
    }
}
