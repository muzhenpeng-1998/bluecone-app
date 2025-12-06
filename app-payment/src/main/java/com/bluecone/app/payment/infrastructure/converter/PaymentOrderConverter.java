package com.bluecone.app.payment.infrastructure.converter;

import com.bluecone.app.payment.domain.enums.PaymentChannel;
import com.bluecone.app.payment.domain.enums.PaymentMethod;
import com.bluecone.app.payment.domain.enums.PaymentScene;
import com.bluecone.app.payment.domain.enums.PaymentStatus;
import com.bluecone.app.payment.domain.model.PaymentOrder;
import com.bluecone.app.payment.infrastructure.persistence.PaymentOrderDO;
import java.math.BigDecimal;
import java.util.Collections;

/**
 * 支付单 DO <-> 领域聚合 的转换器。
 *
 * <p>说明：
 * - infra 层负责与 MyBatis-Plus/数据库交互；
 * - domain 层的 PaymentOrder 不感知表结构；
 * - 此类作为 DO 与领域聚合之间的唯一映射通道。
 */
public final class PaymentOrderConverter {

    private PaymentOrderConverter() {
    }

    /**
     * 将持久化对象转换为领域聚合。
     */
    public static PaymentOrder toDomain(PaymentOrderDO doObj) {
        if (doObj == null) {
            return null;
        }
        PaymentChannel channel = PaymentChannel.fromCode(doObj.getPayChannel());
        PaymentScene scene = PaymentScene.fromCode(doObj.getPayScene());
        PaymentMethod method = null; // 当前表未存储 pay_method，可在未来 schema 调整后补充
        PaymentStatus status = PaymentStatus.fromCode(doObj.getStatus());
        BigDecimal payAmount = doObj.getPayAmount();

        return PaymentOrder.builder()
                .id(doObj.getId())
                .tenantId(doObj.getTenantId())
                .storeId(doObj.getStoreId())
                .bizOrderId(doObj.getBusinessOrderId())
                .userId(doObj.getUserId())
                .channel(channel)
                .method(method)
                .scene(scene)
                .currency(doObj.getCurrency())
                .totalAmount(doObj.getTotalAmount())
                // 数据库只有 pay_amount 字段，既作为应付又作为实付的记录，缺少时置零
                .payableAmount(payAmount == null ? BigDecimal.ZERO : payAmount)
                .paidAmount(payAmount == null ? BigDecimal.ZERO : payAmount)
                .discountAmount(doObj.getDiscountAmount())
                .status(status)
                .expireAt(doObj.getExpireTime())
                .paidAt(null) // 表未定义 paid_at 字段，后续 schema 调整后补充
                .channelTradeNo(doObj.getThirdTradeNo())
                .channelContext(Collections.emptyMap())
                .ext(Collections.emptyMap())
                .idempotentKey(null)
                .version(doObj.getVersion() == null ? 0 : doObj.getVersion().intValue())
                .createdAt(doObj.getCreatedAt())
                .createdBy(doObj.getCreatedBy())
                .updatedAt(doObj.getUpdatedAt())
                .updatedBy(doObj.getUpdatedBy())
                .build();
    }

    /**
     * 将领域聚合转换为持久化对象。
     */
    public static PaymentOrderDO toDO(PaymentOrder aggregate) {
        if (aggregate == null) {
            return null;
        }
        PaymentOrderDO doObj = new PaymentOrderDO();
        doObj.setId(aggregate.getId());
        doObj.setTenantId(aggregate.getTenantId());
        doObj.setStoreId(aggregate.getStoreId());
        doObj.setBusinessOrderId(aggregate.getBizOrderId());
        doObj.setUserId(aggregate.getUserId());
        doObj.setPayChannel(aggregate.getChannel() == null ? null : aggregate.getChannel().getCode());
        doObj.setPayScene(aggregate.getScene() == null ? null : aggregate.getScene().getCode());
        doObj.setCurrency(aggregate.getCurrency());
        doObj.setTotalAmount(aggregate.getTotalAmount());
        // 持久化层仅有 pay_amount，可先写入 paidAmount（若为空则使用 payableAmount）
        BigDecimal payAmount = aggregate.getPaidAmount() != null ? aggregate.getPaidAmount() : aggregate.getPayableAmount();
        doObj.setPayAmount(payAmount);
        doObj.setDiscountAmount(aggregate.getDiscountAmount());
        doObj.setStatus(aggregate.getStatus() == null ? null : aggregate.getStatus().getCode());
        doObj.setExpireTime(aggregate.getExpireAt());
        doObj.setThirdTradeNo(aggregate.getChannelTradeNo());
        doObj.setVersion(aggregate.getVersion() == null ? 0L : aggregate.getVersion().longValue());
        doObj.setCreatedAt(aggregate.getCreatedAt());
        doObj.setCreatedBy(aggregate.getCreatedBy());
        doObj.setUpdatedAt(aggregate.getUpdatedAt());
        doObj.setUpdatedBy(aggregate.getUpdatedBy());
        return doObj;
    }
}
