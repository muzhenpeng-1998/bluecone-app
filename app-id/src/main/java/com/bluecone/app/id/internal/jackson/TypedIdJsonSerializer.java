package com.bluecone.app.id.internal.jackson;

import java.io.IOException;

import com.bluecone.app.id.publicid.api.PublicIdCodec;
import com.bluecone.app.id.typed.api.TypedId;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * 将 TypedId 序列化为 PublicId 字符串的 Jackson 序列化器。
 */
public class TypedIdJsonSerializer extends JsonSerializer<TypedId> {

    private final PublicIdCodec codec;

    public TypedIdJsonSerializer(PublicIdCodec codec) {
        this.codec = codec;
    }

    @Override
    public void serialize(TypedId value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        String publicId = codec.encode(value.type(), value.internal()).asString();
        gen.writeString(publicId);
    }
}
