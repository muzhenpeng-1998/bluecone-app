package com.bluecone.app.id.publicid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.bluecone.app.id.api.IdService;
import com.bluecone.app.id.internal.config.BlueconeIdProperties;
import com.bluecone.app.id.core.Ulid128;
import com.bluecone.app.id.internal.core.UlidIdGenerator;
import com.bluecone.app.id.internal.core.UlidIdService;
import com.bluecone.app.id.publicid.api.DecodedPublicId;
import com.bluecone.app.id.publicid.api.PublicId;
import com.bluecone.app.id.publicid.api.PublicIdCodec;
import com.bluecone.app.id.internal.publicid.DefaultPublicIdCodec;

/**
 * PublicIdCodec 编解码与校验逻辑测试。
 */
class PublicIdCodecTest {

    private IdService newIdService() {
        UlidIdGenerator generator = new UlidIdGenerator();
        return new UlidIdService(generator);
    }

    /**
     * ULID_BASE32 格式的 roundtrip 测试。
     */
    @Test
    void ulidBase32Roundtrip() {
        BlueconeIdProperties.PublicId publicIdProps = new BlueconeIdProperties.PublicId();
        publicIdProps.setFormat(BlueconeIdProperties.PublicId.Format.ULID_BASE32);
        publicIdProps.setChecksumEnabled(false);

        PublicIdCodec codec = new DefaultPublicIdCodec(publicIdProps);
        IdService idService = newIdService();

        for (int i = 0; i < 1000; i++) {
            Ulid128 id = idService.nextUlid();
            PublicId publicId = codec.encode("ord", id);
            String value = publicId.asString();

            DecodedPublicId decoded = codec.decode(value);
            assertEquals("ord", decoded.type());
            assertEquals(id.msb(), decoded.id().msb());
            assertEquals(id.lsb(), decoded.id().lsb());
            assertTrue(codec.isValid(value));

            // payload 为 ULID 字符串长度应为 26
            String[] parts = value.split("_");
            assertEquals(2, parts.length);
            assertEquals(26, parts[1].length());
        }
    }

    /**
     * BASE62_128 格式的 roundtrip 测试。
     */
    @Test
    void base62Roundtrip() {
        BlueconeIdProperties.PublicId publicIdProps = new BlueconeIdProperties.PublicId();
        publicIdProps.setFormat(BlueconeIdProperties.PublicId.Format.BASE62_128);
        publicIdProps.setChecksumEnabled(false);

        PublicIdCodec codec = new DefaultPublicIdCodec(publicIdProps);
        IdService idService = newIdService();

        for (int i = 0; i < 1000; i++) {
            Ulid128 id = idService.nextUlid();
            PublicId publicId = codec.encode("ord", id);
            String value = publicId.asString();

            DecodedPublicId decoded = codec.decode(value);
            assertEquals("ord", decoded.type());
            assertEquals(id.msb(), decoded.id().msb());
            assertEquals(id.lsb(), decoded.id().lsb());
            assertTrue(codec.isValid(value));

            String[] parts = value.split("_");
            assertEquals(2, parts.length);
            // Base62 固定长度 22
            assertEquals(22, parts[1].length());
        }
    }

    /**
     * 开启 checksum 时，任意篡改 1 个字符应能被检测出来。
     */
    @Test
    void checksumEnabledDetectsTamper() {
        BlueconeIdProperties.PublicId publicIdProps = new BlueconeIdProperties.PublicId();
        publicIdProps.setFormat(BlueconeIdProperties.PublicId.Format.ULID_BASE32);
        publicIdProps.setChecksumEnabled(true);

        PublicIdCodec codec = new DefaultPublicIdCodec(publicIdProps);
        IdService idService = newIdService();

        Ulid128 id = idService.nextUlid();
        String original = codec.encode("ord", id).asString();

        char sep = publicIdProps.getSeparator().charAt(0);
        char[] chars = original.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] != sep) {
                chars[i] = (chars[i] == '0' ? '1' : '0');
                break;
            }
        }
        String tampered = new String(chars);

        assertThrows(IllegalArgumentException.class, () -> codec.decode(tampered));
        assertFalse(codec.isValid(tampered));
    }

    /**
     * type 校验测试：必须满足 [a-z0-9]{2,10}。
     */
    @Test
    void typeValidation() {
        BlueconeIdProperties.PublicId publicIdProps = new BlueconeIdProperties.PublicId();
        PublicIdCodec codec = new DefaultPublicIdCodec(publicIdProps);
        IdService idService = newIdService();
        Ulid128 id = idService.nextUlid();

        assertThrows(IllegalArgumentException.class, () -> codec.encode("O", id));
        assertThrows(IllegalArgumentException.class, () -> codec.encode("ORd", id));
        assertThrows(IllegalArgumentException.class, () -> codec.encode("thisisaverylongtype", id));

        // decode 时同样需要校验 type
        assertThrows(IllegalArgumentException.class, () -> codec.decode("ORd_123456"));
        assertThrows(IllegalArgumentException.class, () -> codec.decode("o_123456"));
    }
}

