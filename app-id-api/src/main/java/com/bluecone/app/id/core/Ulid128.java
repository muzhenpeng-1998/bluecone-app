package com.bluecone.app.id.core;

import de.huxhorn.sulky.ulid.ULID;

/**
 * 128 位 ULID 强类型表示，内部使用两个 long 存储。
 */
public record Ulid128(long msb, long lsb) {

    /**
     * 转换为 16 字节大端表示，先 msb 后 lsb。
     *
     * @return 长度为 16 的字节数组
     */
    public byte[] toBytes() {
        byte[] bytes = new byte[16];
        writeLongBigEndian(bytes, 0, msb);
        writeLongBigEndian(bytes, 8, lsb);
        return bytes;
    }

    /**
     * 从 16 字节大端表示还原为 Ulid128。
     *
     * @param bytes 输入字节数组，长度必须为 16
     * @return 对应的 Ulid128 对象
     */
    public static Ulid128 fromBytes(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("bytes 不能为空");
        }
        if (bytes.length != 16) {
            throw new IllegalArgumentException("ULID 字节长度必须为 16，实际为 " + bytes.length);
        }
        long msb = readLongBigEndian(bytes, 0);
        long lsb = readLongBigEndian(bytes, 8);
        return new Ulid128(msb, lsb);
    }

    /**
     * 转换为标准 ULID 字符串（26 位）。
     *
     * @return ULID 字符串
     */
    @Override
    public String toString() {
        return new ULID.Value(msb, lsb).toString();
    }

    private static void writeLongBigEndian(byte[] dest, int offset, long value) {
        dest[offset] = (byte) (value >>> 56);
        dest[offset + 1] = (byte) (value >>> 48);
        dest[offset + 2] = (byte) (value >>> 40);
        dest[offset + 3] = (byte) (value >>> 32);
        dest[offset + 4] = (byte) (value >>> 24);
        dest[offset + 5] = (byte) (value >>> 16);
        dest[offset + 6] = (byte) (value >>> 8);
        dest[offset + 7] = (byte) value;
    }

    private static long readLongBigEndian(byte[] src, int offset) {
        return ((long) (src[offset] & 0xFF) << 56)
                | ((long) (src[offset + 1] & 0xFF) << 48)
                | ((long) (src[offset + 2] & 0xFF) << 40)
                | ((long) (src[offset + 3] & 0xFF) << 32)
                | ((long) (src[offset + 4] & 0xFF) << 24)
                | ((long) (src[offset + 5] & 0xFF) << 16)
                | ((long) (src[offset + 6] & 0xFF) << 8)
                | ((long) (src[offset + 7] & 0xFF));
    }
}
