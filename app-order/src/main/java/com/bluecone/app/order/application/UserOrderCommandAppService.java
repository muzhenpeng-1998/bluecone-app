package com.bluecone.app.order.application;

public interface UserOrderCommandAppService {

    /**
     * 小程序用户取消订单。
     */
    void cancelOrder(Long tenantId, Long userId, Long orderId);

    /**
     * 小程序用户删除订单（软删除，仅隐藏用户视图）。
     */
    void deleteOrder(Long tenantId, Long userId, Long orderId);
}
