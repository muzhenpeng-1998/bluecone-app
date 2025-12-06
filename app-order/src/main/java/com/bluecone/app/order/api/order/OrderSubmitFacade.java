package com.bluecone.app.order.api.order;

import com.bluecone.app.order.api.order.dto.OrderSubmitResponse;
import com.bluecone.app.order.api.order.dto.SubmitOrderFromDraftDTO;

/**
 * 提交订单 Facade，封装草稿提交、幂等与出账通知。
 */
public interface OrderSubmitFacade {

    OrderSubmitResponse submitOrderFromCurrentDraft(SubmitOrderFromDraftDTO command);
}
