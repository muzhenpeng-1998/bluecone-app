package com.bluecone.app.id.publicid.core;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.regex.Pattern;

import com.bluecone.app.id.config.BlueconeIdProperties;
import com.bluecone.app.id.core.Ulid128;
import com.bluecone.app.id.publicid.api.DecodedPublicId;
import com.bluecone.app.id.publicid.api.PublicId;
import com.bluecone.app.id.publicid.api.PublicIdCodec;

import de.huxhorn.sulky.ulid.ULID;

/**
 * 默认的 PublicId 编解码实现。
 *
 * <p>支持两种格式：
 * <ul>
 *     <li>ULID_BASE32：{type}{sep}{ULID(26)}[sep{checksum2}]</li>
 *     <li>BASE62_128：{type}{sep}{Base62_22}[sep{checksum2}]</li>
 * </ul>
 * 其中 checksum 为可选的 CRC8 + Crockford Base32 两字符编码。</p>
 */
public class DefaultPublicIdCodec implements PublicIdCodec {

    private static final Pattern TYPE_PATTERN = Pattern.compile("[a-z0-9]{2,10}");

    private final BlueconeIdProperties.PublicId props;

    public DefaultPublicIdCodec(BlueconeIdProperties.PublicId props) {
        this.props = Objects.requireNonNullElseGet(props, BlueconeIdProperties.PublicId::new);
    }

    @Override
    public PublicId encode(String type, Ulid128 id) {
        validateType(type);
        if (id == null) {
            throw new IllegalArgumentException("ULID 不能为空");
        }
        BlueconeIdProperties.PublicId.Format format = props.getFormat();
        String payload;
        if (format == BlueconeIdProperties.PublicId.Format.BASE62_128) {
            payload = Base62.encodeFixed16(id.toBytes());
        } else {
            // 默认为 ULID_BASE32
            payload = id.toString();
        }
        String raw = buildRaw(type, payload);
        return new PublicId(type, raw);
    }

    @Override
    public PublicId encode(String type, byte[] ulidBytes16) {
        if (ulidBytes16 == null || ulidBytes16.length != 16) {
            throw new IllegalArgumentException("ULID 字节数组长度必须为 16");
        }
        Ulid128 id = Ulid128.fromBytes(ulidBytes16);
        return encode(type, id);
    }

    @Override
    public DecodedPublicId decode(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            throw new IllegalArgumentException("publicId 不能为空");
        }
        String separator = props.getSeparator();
        if (separator == null || separator.isEmpty()) {
            throw new IllegalStateException("PublicId 分隔符不能为空，请检查配置 bluecone.id.public-id.separator");
        }

        String[] parts = publicId.split(Pattern.quote(separator));
        if (parts.length < 2 || parts.length > 3) {
            throw new IllegalArgumentException("publicId 格式非法，期望为 type" + separator + "payload 或带校验和的三段形式");
        }

        String type = parts[0];
        String payload = parts[1];
        String checksumPart = parts.length == 3 ? parts[2] : null;

        validateType(type);

        String base = type + separator + payload;
        if (checksumPart != null) {
            int expected = Base32Crockford.decode2CharsToByte(checksumPart);
            int actual = Crc8.of(base.getBytes(StandardCharsets.UTF_8));
            if (expected != actual) {
                throw new IllegalArgumentException("PublicId 校验失败：checksum 不匹配");
            }
        }

        BlueconeIdProperties.PublicId.Format format = props.getFormat();
        Ulid128 id;
        try {
            if (format == BlueconeIdProperties.PublicId.Format.BASE62_128) {
                byte[] bytes = Base62.decodeToFixed16(payload);
                id = Ulid128.fromBytes(bytes);
            } else {
                // 默认为 ULID_BASE32
                ULID.Value value = ULID.parseULID(payload);
                id = new Ulid128(value.getMostSignificantBits(), value.getLeastSignificantBits());
            }
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("PublicId 解析失败：payload 非法", ex);
        }

        return new DecodedPublicId(type, id);
    }

    @Override
    public boolean isValid(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return false;
        }
        try {
            decode(publicId);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private void validateType(String type) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("PublicId type 不能为空");
        }
        if (!TYPE_PATTERN.matcher(type).matches()) {
            throw new IllegalArgumentException("PublicId type 非法：" + type + "，必须满足正则 [a-z0-9]{2,10}");
        }
    }

    private String buildRaw(String type, String payload) {
        String separator = props.getSeparator();
        if (separator == null || separator.isEmpty()) {
            throw new IllegalStateException("PublicId 分隔符不能为空，请检查配置 bluecone.id.public-id.separator");
        }
        String raw = type + separator + payload;
        if (props.isChecksumEnabled()) {
            int checksum = Crc8.of(raw.getBytes(StandardCharsets.UTF_8));
            String checksumStr = Base32Crockford.encodeByteTo2Chars(checksum);
            raw = raw + separator + checksumStr;
        }
        return raw;
    }
}

