package com.bluecone.app.id.core;

import java.util.Objects;

import com.bluecone.app.id.api.ResourceType;
import com.bluecone.app.id.publicid.api.PublicIdCodec;

/**
 * PublicId 生成工厂。
 *
 * <p>当前策略基于 ULID：{@code {prefix}{separator}{payload}[{separator}{checksum}]}</p>
 * <p>具体格式（分隔符、小写、校验和等）由 {@link PublicIdCodec} 与
 * {@code bluecone.id.public-id.*} 配置共同决定。</p>
 */
public final class PublicIdFactory {

    private final PublicIdCodec codec;

    public PublicIdFactory(PublicIdCodec codec) {
        this.codec = Objects.requireNonNull(codec, "codec 不能为空");
    }

    /**
     * 为给定资源类型和 ULID 生成对外公开 ID。
     *
     * @param type 资源类型
     * @param id   内部 ULID
     * @return 对外公开 ID 字符串
     */
    public String create(ResourceType type, Ulid128 id) {
        if (type == null) {
            throw new IllegalArgumentException("ResourceType 不能为空");
        }
        if (id == null) {
            throw new IllegalArgumentException("Ulid128 不能为空");
        }
        return codec.encode(type.prefix(), id).asString();
    }
}

