package com.bluecone.app.id.publicid.core;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * 简单的 Base62 编解码工具，仅支持 128 位（16 字节）固定长度数据。
 *
 * <p>alphabet：0-9A-Za-z，共 62 个字符。</p>
 */
public final class Base62 {

    private static final char[] ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final int[] REVERSE = new int[128];

    /**
     * 128 位（16 字节）固定长度编码后的 Base62 字符串长度。
     */
    public static final int BASE62_LEN_128 = 22;

    static {
        Arrays.fill(REVERSE, -1);
        for (int i = 0; i < ALPHABET.length; i++) {
            char c = ALPHABET[i];
            if (c < REVERSE.length) {
                REVERSE[c] = i;
            }
        }
    }

    private Base62() {
    }

    /**
     * 将 16 字节数据编码为固定长度 Base62 字符串（22 字符）。
     *
     * @param bytes16 长度为 16 的字节数组
     * @return 长度为 22 的 Base62 字符串
     */
    public static String encodeFixed16(byte[] bytes16) {
        if (bytes16 == null || bytes16.length != 16) {
            throw new IllegalArgumentException("Base62 只支持长度为 16 的字节数组");
        }
        BigInteger value = new BigInteger(1, bytes16);
        BigInteger base = BigInteger.valueOf(62);

        StringBuilder sb = new StringBuilder();
        if (value.equals(BigInteger.ZERO)) {
            sb.append(ALPHABET[0]);
        } else {
            while (value.signum() > 0) {
                BigInteger[] divRem = value.divideAndRemainder(base);
                int digit = divRem[1].intValue();
                sb.append(ALPHABET[digit]);
                value = divRem[0];
            }
        }

        // 当前 sb 为反序，需要翻转
        sb.reverse();

        // 左侧使用 '0' 进行 padding，保证固定长度
        while (sb.length() < BASE62_LEN_128) {
            sb.insert(0, ALPHABET[0]);
        }
        if (sb.length() > BASE62_LEN_128) {
            throw new IllegalStateException("Base62 编码结果长度超过 128 位所允许范围");
        }
        return sb.toString();
    }

    /**
     * 将固定长度 Base62 字符串解码为 16 字节数组。
     *
     * @param s Base62 字符串
     * @return 长度为 16 的字节数组
     */
    public static byte[] decodeToFixed16(String s) {
        if (s == null) {
            throw new IllegalArgumentException("Base62 字符串不能为空");
        }
        if (s.length() != BASE62_LEN_128) {
            throw new IllegalArgumentException("Base62 字符串长度必须为 " + BASE62_LEN_128 + "，实际为 " + s.length());
        }

        BigInteger base = BigInteger.valueOf(62);
        BigInteger value = BigInteger.ZERO;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= REVERSE.length || REVERSE[c] < 0) {
                throw new IllegalArgumentException("Base62 字符非法: '" + c + "'");
            }
            int digit = REVERSE[c];
            value = value.multiply(base).add(BigInteger.valueOf(digit));
        }

        byte[] raw = value.toByteArray();
        if (raw.length == 16) {
            return raw;
        }
        if (raw.length < 16) {
            byte[] result = new byte[16];
            System.arraycopy(raw, 0, result, 16 - raw.length, raw.length);
            return result;
        }
        // raw.length > 16，可能包含符号位，需要特殊处理
        if (raw.length == 17 && raw[0] == 0) {
            // 去掉符号位
            byte[] result = new byte[16];
            System.arraycopy(raw, 1, result, 0, 16);
            return result;
        }
        throw new IllegalArgumentException("Base62 解码结果超过 16 字节，输入值超出 128 位范围");
    }
}

