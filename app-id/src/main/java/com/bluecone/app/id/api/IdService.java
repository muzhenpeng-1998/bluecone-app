package com.bluecone.app.id.api;

import com.bluecone.app.id.core.Ulid128;

/**
 * 统一 ID 门面接口，提供 ULID / Long ID / PublicId 能力。
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

    /**
     * 生成下一个基于 Snowflake 的 long 型 ID。
     *
     * <p>默认实现抛出 {@link UnsupportedOperationException}，仅在启用 long ID 支持时可用。</p>
     *
     * @return 下一个 long 型 ID
     */
    default long nextLongId() {
        throw new UnsupportedOperationException("Long ID generation is not supported by this IdService");
    }

    /**
     * 生成下一个对外公开 ID（PublicId），包含资源类型前缀。
     *
     * <p>默认实现抛出 {@link UnsupportedOperationException}，仅在启用 PublicId 支持时可用。</p>
     *
     * @param type 业务资源类型
     * @return 对外公开 ID 字符串
     */
    default String nextPublicId(ResourceType type) {
        throw new UnsupportedOperationException("PublicId generation is not supported by this IdService");
    }
}
