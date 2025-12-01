package com.bluecone.app.core.notify;

import com.bluecone.app.core.domain.Order;

import java.util.Map;

/**
 * 通知平台对业务暴露的唯一入口（API 层）。
 *
 * <p>业务方只需要构造 {@link NotificationRequest} 即可，无需关心通道路由、限流、Outbox 等细节。</p>
 */
public interface NotificationFacade {

    /**
     * 发送一条通用通知。
     *
     * @param request 通用通知入参
     * @return 平台是否接受（不代表送达）
     */
    NotificationResponse send(NotificationRequest request);

    /**
     * 语义化示例：订单支付通知店主，内部会封装场景码与属性。
     *
     * @param order            订单领域对象
     * @param tenantId         租户 ID
     * @param operatorUserId   触发人
     * @return 平台是否接受
     */
    default NotificationResponse notifyOrderPaidForShopOwner(Order order, Long tenantId, Long operatorUserId) {
        NotificationRequest request = new NotificationRequest(
                NotificationScenario.ORDER_PAID_SHOP_OWNER.getCode(),
                tenantId,
                operatorUserId,
                NotificationScenario.ORDER_PAID_SHOP_OWNER.getDefaultPriority(),
                Map.of(
                        "orderId", order != null ? order.getId() : null,
                        "amount", order != null ? order.getAmount() : null
                ),
                null
        );
        return send(request);
    }
}
