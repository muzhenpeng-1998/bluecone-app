package com.bluecone.app.id.typed.api;

import com.bluecone.app.id.core.Ulid128;
import com.bluecone.app.id.publicid.api.PublicIdCodec;

/**
 * 强类型 ID 抽象，封装内部 ULID 与类型前缀信息。
 *
 * <p>只在内部使用，外部暴露建议通过 public_id 完成。</p>
 */
public sealed interface TypedId
        permits TenantId, StoreId, OrderId, UserId, PaymentId {

    /**
     * 内部 ULID 表示，用于数据库与领域模型。
     *
     * @return 内部 128 位 ULID
     */
    Ulid128 internal();

    /**
     * 类型前缀，例如 ten/sto/ord/usr/pay。
     *
     * @return 类型前缀字符串
     */
    String type();

    /**
     * 使用给定的 PublicIdCodec 生成对外 public_id 字符串。
     *
     * @param codec PublicId 编解码器
     * @return public_id 字符串
     */
    default String asPublic(PublicIdCodec codec) {
        return codec.encode(type(), internal()).asString();
    }
}

