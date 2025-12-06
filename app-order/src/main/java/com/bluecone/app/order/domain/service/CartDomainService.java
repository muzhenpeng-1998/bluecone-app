package com.bluecone.app.order.domain.service;

import com.bluecone.app.order.api.dto.ConfirmOrderItemDTO;
import com.bluecone.app.order.domain.model.Order;
import java.util.Map;

/**
 * 购物车领域服务：购物车等同于订单草稿（Order.status = DRAFT），封装所有对草稿订单的编辑操作。
 */
public interface CartDomainService {

    /**
     * 向草稿订单中新增或合并一个明细行。
     *
     * @param draft 草稿订单聚合（必须为 DRAFT 状态）
     * @param itemDTO 客户端提交的明细 DTO
     * @return 更新后的草稿订单聚合
     */
    Order addItem(Order draft, ConfirmOrderItemDTO itemDTO);

    /**
     * 修改某个 SKU 在购物车中的数量。
     *
     * @param draft 草稿订单聚合
     * @param skuId SKU ID
     * @param attrs 自定义属性（口味/规格等），与 OrderItem.attrs 对齐
     * @param newQuantity 新数量
     * @return 更新后的草稿订单聚合
     */
    Order changeItemQuantity(Order draft, Long skuId, Map<String, Object> attrs, int newQuantity);

    /**
     * 从购物车中移除一个明细行。
     */
    Order removeItem(Order draft, Long skuId, Map<String, Object> attrs);

    /**
     * 清空购物车明细。
     */
    Order clearCart(Order draft);
}
