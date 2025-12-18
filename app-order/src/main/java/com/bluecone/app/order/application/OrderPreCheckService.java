package com.bluecone.app.order.application;

import java.time.LocalDateTime;

/**
 * 订单前置校验服务。
 * <p>职责：在下单前校验门店是否可接单，确保订单提交前进行必要的业务规则检查。</p>
 * <p>依赖：通过 StoreFacade 调用门店能力，保持模块边界清晰。</p>
 */
public interface OrderPreCheckService {

    /**
     * 订单提交前置校验。
     * <p>校验门店是否可接单，包括门店状态、接单开关、渠道能力、营业时间等。</p>
     * <p>如果校验失败，抛出 BizException，异常信息中携带 reasonCode 用于前端提示和埋点统计。</p>
     *
     * @param tenantId     租户 ID
     * @param storeId      门店 ID
     * @param channelType  渠道类型（可选，如提供则校验渠道绑定状态）
     * @param now          当前时间（可选，默认使用当前系统时间）
     * @param cartSummary  购物车摘要（预留扩展，可用于库存、限购等校验）
     * @throws com.bluecone.app.core.exception.BizException 当门店不可接单时抛出，异常中携带 reasonCode
     */
    void preCheck(Long tenantId, Long storeId, String channelType, LocalDateTime now, CartSummary cartSummary);

    /**
     * 购物车摘要（预留扩展）。
     */
    interface CartSummary {
        // 预留：可用于库存校验、限购校验等
    }
}
