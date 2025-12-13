package com.bluecone.app.id.core;

import com.bluecone.app.id.api.IdService;

import de.huxhorn.sulky.ulid.ULID;

/**
 * 基于 UlidIdGenerator 的统一 ID 服务实现。
 */
public class UlidIdService implements IdService {

    private final UlidIdGenerator ulidIdGenerator;

    /**
     * 通过构造器注入底层 ULID 生成器。
     *
     * @param ulidIdGenerator ULID 生成器
     */
    public UlidIdService(UlidIdGenerator ulidIdGenerator) {
        this.ulidIdGenerator = ulidIdGenerator;
    }

    /**
     * 生成下一个 ULID，返回强类型 128 位表示。
     *
     * @return 下一个 ULID 对象
     */
    @Override
    public Ulid128 nextUlid() {
        ULID.Value value = ulidIdGenerator.nextValue();
        return new Ulid128(value.getMostSignificantBits(), value.getLeastSignificantBits());
    }

    /**
     * 生成下一个 ULID 字符串（26 位）。
     *
     * @return ULID 字符串
     */
    @Override
    public String nextUlidString() {
        return ulidIdGenerator.nextUlid();
    }

    /**
     * 生成下一个 ULID 的二进制表示（16 字节，大端）。
     *
     * @return 16 字节数组，前 8 字节为 MSB，后 8 字节为 LSB
     */
    @Override
    public byte[] nextUlidBytes() {
        return nextUlid().toBytes();
    }
}

