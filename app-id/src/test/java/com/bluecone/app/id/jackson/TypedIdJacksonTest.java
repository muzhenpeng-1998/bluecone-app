package com.bluecone.app.id.jackson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.bluecone.app.id.api.IdService;
import com.bluecone.app.id.config.BlueconeIdProperties;
import com.bluecone.app.id.core.UlidIdGenerator;
import com.bluecone.app.id.core.UlidIdService;
import com.bluecone.app.id.publicid.api.PublicIdCodec;
import com.bluecone.app.id.publicid.core.DefaultPublicIdCodec;
import com.bluecone.app.id.typed.api.OrderId;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * TypedId Jackson 序列化行为测试。
 */
class TypedIdJacksonTest {

    private IdService newIdService() {
        UlidIdGenerator generator = new UlidIdGenerator();
        return new UlidIdService(generator);
    }

    private PublicIdCodec newCodec() {
        BlueconeIdProperties.PublicId props = new BlueconeIdProperties.PublicId();
        return new DefaultPublicIdCodec(props);
    }

    private record Resp(OrderId orderId) {
    }

    @Test
    void typedIdShouldSerializeToPublicIdString() throws Exception {
        IdService idService = newIdService();
        PublicIdCodec codec = newCodec();

        OrderId orderId = new OrderId(idService.nextUlid());
        String expectedPublic = codec.encode("ord", orderId.internal()).asString();

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new BlueconeIdJacksonModule(codec));

        String json = mapper.writeValueAsString(new Resp(orderId));

        assertTrue(json.contains("\"orderId\""));
        assertTrue(json.contains(expectedPublic));
        assertEquals("{\"orderId\":\"" + expectedPublic + "\"}", json);
    }
}
