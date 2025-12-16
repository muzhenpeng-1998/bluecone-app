package com.bluecone.app.id.internal.jackson;

import com.bluecone.app.id.core.Ulid128;
import com.bluecone.app.id.publicid.api.PublicIdCodec;
import com.bluecone.app.id.typed.api.TypedId;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * BlueCone ID 相关的 Jackson Module，统一 TypedId 与 ULID 的序列化/反序列化行为。
 */
public class BlueconeIdJacksonModule extends SimpleModule {

    public BlueconeIdJacksonModule(PublicIdCodec codec) {
        super("BlueconeIdJacksonModule");
        // TypedId 统一序列化为 public_id 字符串
        addSerializer(TypedId.class, new TypedIdJsonSerializer(codec));
        // Ulid128 统一序列化/反序列化为标准 26 位字符串
        addSerializer(Ulid128.class, new Ulid128JacksonModule.Ulid128Serializer());
        addDeserializer(Ulid128.class, new Ulid128JacksonModule.Ulid128Deserializer());
    }
}
