package com.bluecone.app.order.application;

import com.bluecone.app.order.api.dto.UserOrderRefundRequest;

public interface UserOrderRefundAppService {

    /**
     * 小程序用户发起退款申请。
     */
    void applyRefund(Long orderId, UserOrderRefundRequest request);
}
