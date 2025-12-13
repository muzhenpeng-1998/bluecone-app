package com.bluecone.app.id.jackson;

import java.io.IOException;

import com.bluecone.app.id.core.Ulid128;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import de.huxhorn.sulky.ulid.ULID;

/**
 * Ulid128 的 Jackson 序列化模块。
 *
 * <p>序列化：{@link Ulid128} -&gt; 26 位 ULID 字符串。</p>
 * <p>反序列化：26 位 ULID 字符串 -&gt; {@link Ulid128}，非法输入抛出 {@link JsonMappingException}。</p>
 */
public class Ulid128JacksonModule extends SimpleModule {

    public Ulid128JacksonModule() {
        super("Ulid128JacksonModule");
        addSerializer(Ulid128.class, new Ulid128Serializer());
        addDeserializer(Ulid128.class, new Ulid128Deserializer());
    }

    /**
     * Ulid128 -> ULID 字符串序列化器。
     */
    public static class Ulid128Serializer extends JsonSerializer<Ulid128> {

        @Override
        public void serialize(Ulid128 value, com.fasterxml.jackson.core.JsonGenerator gen,
                              SerializerProvider serializers) throws IOException {
            if (value == null) {
                gen.writeNull();
            } else {
                gen.writeString(value.toString());
            }
        }
    }

    /**
     * ULID 字符串 -> Ulid128 反序列化器。
     */
    public static class Ulid128Deserializer extends JsonDeserializer<Ulid128> {

        @Override
        public Ulid128 deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonToken token = p.currentToken();
            if (token == JsonToken.VALUE_NULL) {
                return null;
            }
            if (token != JsonToken.VALUE_STRING) {
                throw JsonMappingException.from(p, "Ulid128 必须从字符串反序列化");
            }
            String text = p.getText();
            if (text == null || text.isBlank()) {
                return null;
            }
            try {
                ULID.Value value = ULID.parseULID(text);
                return new Ulid128(value.getMostSignificantBits(), value.getLeastSignificantBits());
            } catch (IllegalArgumentException ex) {
                throw JsonMappingException.from(p, "无效的 ULID 字符串: " + text, ex);
            }
        }
    }
}

