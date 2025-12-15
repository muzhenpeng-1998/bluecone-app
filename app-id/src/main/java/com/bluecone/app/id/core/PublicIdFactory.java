package com.bluecone.app.id.core;

import java.util.Objects;

import com.bluecone.app.id.api.ResourceType;
import com.bluecone.app.id.publicid.api.PublicIdCodec;

/**
 * PublicId 生成工厂。
 *
 * <p>当前策略基于 ULID：{@code {prefix}{separator}{payload}[{separator}{checksum}]}</p>
 * <p>具体格式（分隔符、小写、校验和等）由 {@link PublicIdCodec} 与
 * {@code bluecone.id.public-id.*} 配置共同决定。</p>
 */
public final class PublicIdFactory {

    private final PublicIdCodec codec;

    public PublicIdFactory(PublicIdCodec codec) {
        this.codec = Objects.requireNonNull(codec, "codec 不能为空");
    }

    /**
     * 为给定资源类型和 ULID 生成对外公开 ID。
     *
     * @param type 资源类型
     * @param id   内部 ULID
     * @return 对外公开 ID 字符串
     */
    public String create(ResourceType type, Ulid128 id) {
        if (type == null) {
            throw new IllegalArgumentException("ResourceType 不能为空");
        }
        if (id == null) {
            throw new IllegalArgumentException("Ulid128 不能为空");
        }
        return codec.encode(type.prefix(), id).asString();
    }
    
    /**
     * 校验 Public ID 的格式和类型是否合法。
     * 
     * <p>校验内容：
     * <ul>
     *   <li>格式：prefix_ulid（下划线分隔）</li>
     *   <li>前缀：是否匹配预期资源类型</li>
     *   <li>ULID：26 位字符合法性</li>
     * </ul>
     *
     * @param expectedType 预期的资源类型
     * @param publicId 待校验的 Public ID
     * @throws IllegalArgumentException 如果格式非法、类型不匹配或 ULID 非法
     */
    public void validate(ResourceType expectedType, String publicId) {
        if (expectedType == null) {
            throw new IllegalArgumentException("expectedType 不能为空");
        }
        if (publicId == null || publicId.isEmpty()) {
            throw new IllegalArgumentException("publicId 不能为空");
        }
        
        // 1. 格式校验：必须包含下划线分隔符
        int separatorIndex = publicId.indexOf('_');
        if (separatorIndex <= 0) {
            throw new IllegalArgumentException(
                String.format("Public ID 格式非法，缺少分隔符 '_': %s", publicId)
            );
        }
        
        // 2. 前缀校验
        String prefix = publicId.substring(0, separatorIndex);
        if (!prefix.equals(expectedType.prefix())) {
            throw new IllegalArgumentException(
                String.format("Public ID 类型不匹配，期望前缀: %s，实际前缀: %s", 
                              expectedType.prefix(), prefix)
            );
        }
        
        // 3. ULID 部分校验
        String ulidPart = publicId.substring(separatorIndex + 1);
        if (ulidPart.length() != 26) {
            throw new IllegalArgumentException(
                String.format("ULID 长度非法，期望 26 位，实际 %d 位: %s", 
                              ulidPart.length(), ulidPart)
            );
        }
        
        // 4. ULID 字符合法性校验（Crockford Base32）
        if (!ulidPart.matches("^[0-9A-HJKMNP-TV-Z]{26}$")) {
            throw new IllegalArgumentException(
                String.format("ULID 包含非法字符（仅允许 Crockford Base32）: %s", ulidPart)
            );
        }
    }
}

