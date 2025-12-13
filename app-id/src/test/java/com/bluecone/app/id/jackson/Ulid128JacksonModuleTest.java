package com.bluecone.app.id.jackson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bluecone.app.id.core.Ulid128;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/**
 * Ulid128JacksonModule 行为测试。
 */
class Ulid128JacksonModuleTest {

    @Test
    void ulid128ShouldRoundTripAsUlidString() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Ulid128JacksonModule());

        Ulid128 original = new Ulid128(0x1234_5678_9ABCDEFL, 0x0FED_CBA9_8765_4321L);

        String json = mapper.writeValueAsString(original);
        Ulid128 restored = mapper.readValue(json, Ulid128.class);

        assertEquals(original.msb(), restored.msb());
        assertEquals(original.lsb(), restored.lsb());
    }

    @Test
    void invalidUlidStringShouldFailDeserialization() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Ulid128JacksonModule());

        String json = "\"NOT-VALID-ULID\"";

        assertThrows(JsonProcessingException.class, () -> mapper.readValue(json, Ulid128.class));
    }
}

