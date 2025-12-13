package com.bluecone.app.id.api;

import com.bluecone.app.id.core.Ulid128;

/**
 * 统一 ID 门面接口，提供 ULID 相关能力。
 */
public interface IdService {

    /**
     * 生成下一个 ULID，返回强类型 128 位表示。
     *
     * @return 下一个 ULID 对象
     */
    Ulid128 nextUlid();

    /**
     * 生成下一个 ULID 字符串（26 位）。
     *
     * @return ULID 字符串
     */
    String nextUlidString();

    /**
     * 生成下一个 ULID 的二进制表示（16 字节，大端）。
     *
     * @return 16 字节数组，前 8 字节为 MSB，后 8 字节为 LSB
     */
    byte[] nextUlidBytes();
}

