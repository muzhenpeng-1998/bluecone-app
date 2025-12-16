package com.bluecone.app.id.internal.core;

import com.bluecone.app.id.api.IdScope;
import com.bluecone.app.id.api.IdService;
import com.bluecone.app.id.api.ResourceType;
import com.bluecone.app.id.core.Ulid128;

import de.huxhorn.sulky.ulid.ULID;

/**
 * 基于 UlidIdGenerator 的统一 ID 服务实现。
 *
 * <p>在原有 ULID 能力基础上，扩展支持 Snowflake long ID 与 PublicId 生成。</p>
 */
public class UlidIdService implements IdService {

    private final UlidIdGenerator ulidIdGenerator;
    private final SnowflakeLongIdGenerator longIdGenerator;
    private final PublicIdFactory publicIdFactory;

    /**
     * 仅依赖 ULID 生成器的构造器，保留历史用法。
     *
     * @param ulidIdGenerator ULID 生成器
     */
    public UlidIdService(UlidIdGenerator ulidIdGenerator) {
        this(ulidIdGenerator, null, null);
    }

    /**
     * 完整构造器，可注入 Snowflake long ID 生成器与 PublicId 工厂。
     *
     * @param ulidIdGenerator  ULID 生成器
     * @param longIdGenerator  Snowflake long ID 生成器（可选）
     * @param publicIdFactory  PublicId 工厂（可选）
     */
    public UlidIdService(UlidIdGenerator ulidIdGenerator,
                         SnowflakeLongIdGenerator longIdGenerator,
                         PublicIdFactory publicIdFactory) {
        this.ulidIdGenerator = ulidIdGenerator;
        this.longIdGenerator = longIdGenerator;
        this.publicIdFactory = publicIdFactory;
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

    /**
     * 基于 Snowflake 算法生成下一个 long 型 ID。
     *
     * @return long 型 ID
     * @deprecated 使用 nextLong(IdScope) 替代
     */
    @Deprecated
    public long nextLongId() {
        if (longIdGenerator == null) {
            throw new UnsupportedOperationException("Long ID generation is not supported by this IdService");
        }
        return longIdGenerator.nextId();
    }
    
    /**
     * 基于号段模式生成下一个 long 型 ID。
     * 
     * <p>注意：此实现使用 Snowflake，不支持 scope 参数。
     * 如需号段模式，请使用 EnhancedIdService。
     *
     * @param scope ID 作用域（此实现忽略该参数）
     * @return long 型 ID
     */
    @Override
    public long nextLong(IdScope scope) {
        if (longIdGenerator == null) {
            throw new UnsupportedOperationException("Long ID generation is not supported by this IdService");
        }
        return longIdGenerator.nextId();
    }

    /**
     * 为给定资源类型生成对外公开 ID。
     *
     * @param type 资源类型
     * @return PublicId 字符串
     */
    @Override
    public String nextPublicId(ResourceType type) {
        if (publicIdFactory == null) {
            throw new UnsupportedOperationException("PublicId generation is not supported by this IdService");
        }
        Ulid128 ulid = nextUlid();
        return publicIdFactory.create(type, ulid);
    }
    
    /**
     * 校验 Public ID 的格式和类型是否合法。
     *
     * @param expectedType 预期的资源类型
     * @param publicId 待校验的 Public ID
     * @throws IllegalArgumentException 如果格式非法、类型不匹配或 ULID 非法
     */
    @Override
    public void validatePublicId(ResourceType expectedType, String publicId) {
        if (publicIdFactory == null) {
            throw new UnsupportedOperationException("PublicId validation is not supported by this IdService");
        }
        publicIdFactory.validate(expectedType, publicId);
    }
}
