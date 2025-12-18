package com.bluecone.app.order.application;

import com.bluecone.app.order.api.dto.OrderConfirmRequest;
import com.bluecone.app.order.api.dto.OrderConfirmResponse;

/**
 * 订单确认单应用服务接口。
 * <p>负责订单确认单的业务编排，包括门店校验、商品校验、价格计算等。</p>
 */
public interface OrderConfirmApplicationService {

    /**
     * 订单确认单（M0）。
     * <p>业务流程：</p>
     * <ol>
     *   <li>调用门店 precheck（复用已完成能力）</li>
     *   <li>调用商品校验（存在则复用，不存在则暂时跳过，M0可不做）</li>
     *   <li>计算价格（M0可不做优惠，但要预留 PromotionFacade 接口位，默认 no-op）</li>
     *   <li>返回 confirmToken/priceVersion（后续 submit 校验用）</li>
     * </ol>
     *
     * @param request 确认单请求
     * @return 确认单响应（包含价格、门店可接单状态、confirmToken等）
     */
    OrderConfirmResponse confirm(OrderConfirmRequest request);
}
