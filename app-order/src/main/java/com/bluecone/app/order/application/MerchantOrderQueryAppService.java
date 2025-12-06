package com.bluecone.app.order.application;

import com.bluecone.app.order.api.dto.MerchantOrderDetailView;
import com.bluecone.app.order.api.dto.MerchantOrderListQuery;
import com.bluecone.app.order.api.dto.MerchantOrderSummaryView;
import com.bluecone.app.core.user.domain.member.repository.read.PageResult;

public interface MerchantOrderQueryAppService {

    PageResult<MerchantOrderSummaryView> listStoreOrders(MerchantOrderListQuery query);

    MerchantOrderDetailView getStoreOrderDetail(Long tenantId, Long storeId, Long operatorId, Long orderId);
}
