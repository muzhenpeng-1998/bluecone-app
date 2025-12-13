package com.bluecone.app.id.typed.core;

import java.util.function.Function;

import com.bluecone.app.id.api.IdService;
import com.bluecone.app.id.core.Ulid128;
import com.bluecone.app.id.publicid.api.DecodedPublicId;
import com.bluecone.app.id.publicid.api.PublicIdCodec;
import com.bluecone.app.id.typed.api.OrderId;
import com.bluecone.app.id.typed.api.PaymentId;
import com.bluecone.app.id.typed.api.StoreId;
import com.bluecone.app.id.typed.api.TenantId;
import com.bluecone.app.id.typed.api.TypedId;
import com.bluecone.app.id.typed.api.UserId;

/**
 * 强类型 ID 工具类，提供创建与从 public_id 还原的统一范式。
 */
public final class TypedIds {

    private TypedIds() {
    }

    public static TenantId newTenantId(IdService ids) {
        return new TenantId(ids.nextUlid());
    }

    public static StoreId newStoreId(IdService ids) {
        return new StoreId(ids.nextUlid());
    }

    public static OrderId newOrderId(IdService ids) {
        return new OrderId(ids.nextUlid());
    }

    public static UserId newUserId(IdService ids) {
        return new UserId(ids.nextUlid());
    }

    public static PaymentId newPaymentId(IdService ids) {
        return new PaymentId(ids.nextUlid());
    }

    /**
     * 从 public_id 解析出内部 TypedId。
     *
     * @param publicId     公开 ID 字符串
     * @param codec        PublicId 编解码器
     * @param ctor         将 ULID 转为具体 TypedId 的构造函数
     * @param expectedType 期望的类型前缀（如 ord/sto）
     * @param <T>          TypedId 子类型
     * @return 解析出的强类型 ID
     */
    public static <T extends TypedId> T fromPublic(String publicId,
                                                   PublicIdCodec codec,
                                                   Function<Ulid128, T> ctor,
                                                   String expectedType) {
        DecodedPublicId decoded = codec.decode(publicId);
        String actualType = decoded.type();
        if (!expectedType.equals(actualType)) {
            throw new IllegalArgumentException(
                    "public_id 类型不匹配，期望=" + expectedType + "，实际=" + actualType);
        }
        return ctor.apply(decoded.id());
    }

    /**
     * 将 TypedId 转为内部 ULID，便于持久化层使用。
     */
    public static Ulid128 toInternal(TypedId id) {
        return id == null ? null : id.internal();
    }

    /**
     * 从内部 ULID 还原 TypedId。
     */
    public static <T extends TypedId> T fromInternal(Ulid128 ulid, Function<Ulid128, T> ctor) {
        return ulid == null ? null : ctor.apply(ulid);
    }
}

