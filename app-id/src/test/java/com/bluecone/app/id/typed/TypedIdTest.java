package com.bluecone.app.id.typed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.bluecone.app.id.api.IdService;
import com.bluecone.app.id.internal.config.BlueconeIdProperties;
import com.bluecone.app.id.core.Ulid128;
import com.bluecone.app.id.internal.core.UlidIdGenerator;
import com.bluecone.app.id.internal.core.UlidIdService;
import com.bluecone.app.id.publicid.api.DecodedPublicId;
import com.bluecone.app.id.publicid.api.PublicIdCodec;
import com.bluecone.app.id.internal.publicid.DefaultPublicIdCodec;
import com.bluecone.app.id.typed.api.OrderId;
import com.bluecone.app.id.typed.api.StoreId;
import com.bluecone.app.id.internal.typed.TypedIds;

/**
 * TypedId 基础行为测试。
 */
class TypedIdTest {

    private IdService newIdService() {
        UlidIdGenerator generator = new UlidIdGenerator();
        return new UlidIdService(generator);
    }

    private PublicIdCodec newCodec() {
        BlueconeIdProperties.PublicId props = new BlueconeIdProperties.PublicId();
        return new DefaultPublicIdCodec(props);
    }

    @Test
    void orderIdBasicAndAsPublicRoundtrip() {
        IdService idService = newIdService();
        PublicIdCodec codec = newCodec();

        Ulid128 ulid = idService.nextUlid();
        OrderId orderId = new OrderId(ulid);

        assertEquals("ord", orderId.type());
        assertEquals(ulid, orderId.internal());

        String publicId = orderId.asPublic(codec);
        DecodedPublicId decoded = codec.decode(publicId);
        assertEquals("ord", decoded.type());
        assertEquals(ulid.msb(), decoded.id().msb());
        assertEquals(ulid.lsb(), decoded.id().lsb());

        OrderId orderId2 = new OrderId(ulid);
        assertEquals(orderId, orderId2);
    }

    @Test
    void fromPublicWithExpectedTypeShouldWorkOrFailOnMismatch() {
        IdService idService = newIdService();
        PublicIdCodec codec = newCodec();

        Ulid128 ulid = idService.nextUlid();
        OrderId orderId = new OrderId(ulid);
        String publicId = orderId.asPublic(codec);

        OrderId parsed = TypedIds.fromPublic(publicId, codec, OrderId::new, "ord");
        assertEquals(orderId, parsed);

        // 类型不匹配时应抛异常
        assertThrows(IllegalArgumentException.class,
                () -> TypedIds.fromPublic(publicId, codec, StoreId::new, "sto"));
    }
}

