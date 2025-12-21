package com.bluecone.app.id.internal.jackson;

import com.bluecone.app.id.core.Ulid128;
import com.bluecone.app.id.publicid.api.PublicIdCodec;
import com.bluecone.app.id.typed.api.TypedId;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * BlueCone ID 相关的 Jackson Module，统一 TypedId 与 ULID 的序列化/反序列化行为。
 */
public class BlueconeIdJacksonModule extends SimpleModule {

    /**
     * 构造 Jackson 模块。
     * 
     * @param codec PublicIdCodec（可为 null，为 null 时不注册 TypedId 序列化器）
     */
    public BlueconeIdJacksonModule(PublicIdCodec codec) {
        super("BlueconeIdJacksonModule");
        // TypedId 统一序列化为 public_id 字符串（仅在 codec 可用时注册）
        if (codec != null) {
            addSerializer(TypedId.class, new TypedIdJsonSerializer(codec));
        }
        // Ulid128 统一序列化/反序列化为标准 26 位字符串
        addSerializer(Ulid128.class, new Ulid128JacksonModule.Ulid128Serializer());
        addDeserializer(Ulid128.class, new Ulid128JacksonModule.Ulid128Deserializer());
    }
}
