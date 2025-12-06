package com.bluecone.app.order.api.cart;

import com.bluecone.app.order.api.cart.dto.AddDraftItemCommandDTO;
import com.bluecone.app.order.api.cart.dto.ChangeDraftItemQuantityCommandDTO;
import com.bluecone.app.order.api.cart.dto.ClearDraftCommandDTO;
import com.bluecone.app.order.api.cart.dto.LockDraftCommandDTO;
import com.bluecone.app.order.api.cart.dto.OrderDraftViewDTO;
import com.bluecone.app.order.api.cart.dto.RemoveDraftItemCommandDTO;

/**
 * 订单草稿 / 购物车 Facade，面向 app-application 暴露。
 */
public interface OrderDraftFacade {

    OrderDraftViewDTO loadCurrentDraft();

    OrderDraftViewDTO addItem(AddDraftItemCommandDTO command);

    OrderDraftViewDTO changeItemQuantity(ChangeDraftItemQuantityCommandDTO command);

    OrderDraftViewDTO removeItem(RemoveDraftItemCommandDTO command);

    OrderDraftViewDTO clearDraft(ClearDraftCommandDTO command);

    /**
     * 锁定草稿，准备创建正式订单（下单前一步）。
     */
    OrderDraftViewDTO lockDraft(LockDraftCommandDTO command);
}
