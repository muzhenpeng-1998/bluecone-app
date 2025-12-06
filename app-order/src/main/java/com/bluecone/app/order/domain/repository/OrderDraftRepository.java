package com.bluecone.app.order.domain.repository;

import com.bluecone.app.order.domain.model.Order;
import java.util.Optional;

/**
 * 订单草稿 / 购物车仓储。
 *
 * <p>草稿订单必须是 {@link com.bluecone.app.order.domain.enums.OrderStatus#DRAFT}
 *，按租户/门店/用户/渠道/场景唯一定位。</p>
 */
public interface OrderDraftRepository {

    /**
     * 当前上下文下查找草稿订单（可能不存在）。
     *
     * @param tenantId 租户ID（必填）
     * @param storeId 门店ID（必填）
     * @param userId 用户ID（必填）
     * @param channel 渠道
     * @param scene 场景（对应 orderSource）
     */
    Optional<Order> findDraft(Long tenantId,
                              Long storeId,
                              Long userId,
                              String channel,
                              String scene);

    /**
     * 保存草稿订单，支持新增 / 更新（乐观锁）。
     *
     * @param draft 草稿订单，必须为 DRAFT 状态
     */
    Order saveDraft(Order draft);

    /**
     * 删除草稿订单及其明细（通常用于清空购物车）。
     *
     * @param orderId 草稿订单ID
     */
    void deleteDraft(Long orderId);
}
