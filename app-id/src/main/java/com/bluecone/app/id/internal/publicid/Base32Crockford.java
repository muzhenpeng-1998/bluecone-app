package com.bluecone.app.id.internal.publicid;

import java.util.Arrays;

/**
 * Crockford Base32 编码的极简实现，仅用于将单字节值编码为 2 个字符，以及反向解码。
 *
 * <p>alphabet 与 ULID 使用的一致：0123456789ABCDEFGHJKMNPQRSTVWXYZ。</p>
 */
public final class Base32Crockford {

    private static final char[] ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final int[] REVERSE = new int[128];

    static {
        Arrays.fill(REVERSE, -1);
        for (int i = 0; i < ALPHABET.length; i++) {
            char c = ALPHABET[i];
            if (c < REVERSE.length) {
                REVERSE[c] = i;
            }
        }
    }

    private Base32Crockford() {
    }

    /**
     * 将 0..255 的字节值编码为 2 个 Crockford Base32 字符。
     *
     * @param value 字节值（0..255）
     * @return 长度为 2 的字符串
     */
    public static String encodeByteTo2Chars(int value) {
        int v = value & 0xFF;
        int high = (v >>> 5) & 0x1F;
        int low = v & 0x1F;
        return new String(new char[]{ALPHABET[high], ALPHABET[low]});
    }

    /**
     * 将 2 个 Crockford Base32 字符解码为 0..255 的字节值。
     *
     * @param two 长度为 2 的字符串
     * @return 字节值（0..255）
     */
    public static int decode2CharsToByte(String two) {
        if (two == null || two.length() != 2) {
            throw new IllegalArgumentException("校验和必须为 2 个 Crockford Base32 字符");
        }
        char c1 = Character.toUpperCase(two.charAt(0));
        char c2 = Character.toUpperCase(two.charAt(1));

        int v1 = decodeChar(c1);
        int v2 = decodeChar(c2);
        return ((v1 << 5) | v2) & 0xFF;
    }

    private static int decodeChar(char c) {
        if (c >= REVERSE.length) {
            throw new IllegalArgumentException("非法 Crockford Base32 字符: '" + c + "'");
        }
        int v = REVERSE[c];
        if (v < 0) {
            throw new IllegalArgumentException("非法 Crockford Base32 字符: '" + c + "'");
        }
        return v;
    }
}

