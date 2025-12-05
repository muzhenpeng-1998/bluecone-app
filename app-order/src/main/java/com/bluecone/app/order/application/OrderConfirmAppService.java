package com.bluecone.app.order.application;

import com.bluecone.app.order.api.dto.ConfirmOrderRequest;
import com.bluecone.app.order.api.dto.ConfirmOrderResponse;

/**
 * 订单确认应用服务。
 */
public interface OrderConfirmAppService {

    /**
     * 确认订单。
     */
    ConfirmOrderResponse confirmOrder(ConfirmOrderRequest request);
}
