package com.bluecone.app.id.jackson;

import java.io.IOException;

import com.bluecone.app.id.core.Ulid128;
import com.bluecone.app.id.publicid.api.PublicIdCodec;
import com.bluecone.app.id.typed.api.TypedId;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * BlueCone ID 相关的 Jackson Module，统一 TypedId 与 ULID 的序列化行为。
 */
public class BlueconeIdJacksonModule extends SimpleModule {

    public BlueconeIdJacksonModule(PublicIdCodec codec) {
        super("BlueconeIdJacksonModule");
        // TypedId 统一序列化为 public_id 字符串
        addSerializer(TypedId.class, new TypedIdJsonSerializer(codec));
        // 可选：ULID 直接序列化为标准 26 位字符串，便于调试
        addSerializer(Ulid128.class, new JsonSerializer<Ulid128>() {
            @Override
            public void serialize(Ulid128 value, JsonGenerator gen,
                                  SerializerProvider serializers) throws IOException {
                if (value == null) {
                    gen.writeNull();
                } else {
                    gen.writeString(value.toString());
                }
            }
        });
    }
}
