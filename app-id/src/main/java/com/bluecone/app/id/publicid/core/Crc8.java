package com.bluecone.app.id.publicid.core;

/**
 * 简单 CRC8 校验实现，用于 PublicId 校验和。
 *
 * <p>多项式使用 0x07，初始值 0x00。</p>
 */
public final class Crc8 {

    private static final int POLY = 0x07;

    private Crc8() {
    }

    /**
     * 计算 CRC8 校验值。
     *
     * @param data 输入数据
     * @return 校验值，范围 0..255
     */
    public static int of(byte[] data) {
        if (data == null) {
            return 0;
        }
        int crc = 0x00;
        for (byte b : data) {
            crc ^= (b & 0xFF);
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x80) != 0) {
                    crc = ((crc << 1) ^ POLY) & 0xFF;
                } else {
                    crc = (crc << 1) & 0xFF;
                }
            }
        }
        return crc & 0xFF;
    }
}

