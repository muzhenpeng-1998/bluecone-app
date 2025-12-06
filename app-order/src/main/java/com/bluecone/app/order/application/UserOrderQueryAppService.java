package com.bluecone.app.order.application;

import com.bluecone.app.order.api.dto.ConfirmOrderPreviewResponse;

public interface UserOrderQueryAppService {

    /**
     * 再来一单（返回预览结果）。
     */
    ConfirmOrderPreviewResponse reorder(Long tenantId, Long userId, Long orderId);

    com.bluecone.app.core.user.domain.member.repository.read.PageResult<com.bluecone.app.order.api.dto.UserOrderSummaryView> listUserOrders(com.bluecone.app.order.api.dto.UserOrderListQuery query);

    com.bluecone.app.order.api.dto.UserOrderDetailView getUserOrderDetail(Long tenantId, Long userId, Long orderId);
}
