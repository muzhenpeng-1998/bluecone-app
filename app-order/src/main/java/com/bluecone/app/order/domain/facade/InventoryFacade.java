package com.bluecone.app.order.domain.facade;

/**
 * 库存 Facade 接口（订单模块调用库存模块）。
 * 
 * <h3>职责：</h3>
 * <ul>
 *   <li>释放库存预占（订单取消时）</li>
 *   <li>回补库存（订单退款时）</li>
 *   <li>扣减库存（订单完成时，如果采用完成时扣减策略）</li>
 * </ul>
 * 
 * <h3>实现策略：</h3>
 * <ul>
 *   <li>M4 阶段先提供 No-op 实现（不实际操作库存）</li>
 *   <li>后续根据库存策略（预占 vs 完成时扣减）实现真实逻辑</li>
 *   <li>通过事件/Outbox 异步调用，保证最终一致性</li>
 * </ul>
 */
public interface InventoryFacade {
    
    /**
     * 释放库存预占（订单取消时调用）。
     * 
     * <h4>适用场景：</h4>
     * <ul>
     *   <li>订单取消（WAIT_PAY/WAIT_ACCEPT 取消）→ 释放预占</li>
     *   <li>订单退款成功（REFUNDED）→ 若已扣减则回补；若只是预占则释放</li>
     * </ul>
     * 
     * <h4>实现要求：</h4>
     * <ul>
     *   <li>幂等性：同一订单重复释放只执行一次</li>
     *   <li>异步化：通过事件/Outbox 异步调用，避免影响订单主流程</li>
     *   <li>容错性：库存释放失败不影响订单取消，可通过补偿机制修复</li>
     * </ul>
     * 
     * @param tenantId 租户ID
     * @param storeId 门店ID
     * @param orderId 订单ID
     * @param reasonCode 释放原因码（USER_CANCEL、MERCHANT_REJECT、REFUNDED等）
     */
    void releaseReservation(Long tenantId, Long storeId, Long orderId, String reasonCode);
    
    /**
     * 回补库存（订单退款时调用，已扣减的情况）。
     * 
     * <h4>适用场景：</h4>
     * <ul>
     *   <li>订单退款成功（REFUNDED）→ 若已扣减则回补库存</li>
     *   <li>订单完成后退款（COMPLETED → REFUNDED）→ 回补库存</li>
     * </ul>
     * 
     * <h4>实现要求：</h4>
     * <ul>
     *   <li>幂等性：同一订单重复回补只执行一次</li>
     *   <li>异步化：通过事件/Outbox 异步调用，避免影响退款主流程</li>
     *   <li>容错性：库存回补失败不影响退款，可通过补偿机制修复</li>
     * </ul>
     * 
     * @param tenantId 租户ID
     * @param storeId 门店ID
     * @param orderId 订单ID
     * @param reasonCode 回补原因码（REFUNDED、AFTER_SALES_REFUND等）
     */
    void compensateInventory(Long tenantId, Long storeId, Long orderId, String reasonCode);
    
    /**
     * 扣减库存（订单完成时调用，如果采用完成时扣减策略）。
     * 
     * <h4>适用场景：</h4>
     * <ul>
     *   <li>订单完成（READY → COMPLETED）→ 扣减库存（如果采用完成时扣减策略）</li>
     * </ul>
     * 
     * <h4>实现要求：</h4>
     * <ul>
     *   <li>幂等性：同一订单重复扣减只执行一次</li>
     *   <li>异步化：通过事件/Outbox 异步调用，避免影响订单完成流程</li>
     *   <li>容错性：库存扣减失败不影响订单完成，可通过补偿机制修复</li>
     * </ul>
     * 
     * @param tenantId 租户ID
     * @param storeId 门店ID
     * @param orderId 订单ID
     */
    void deductInventory(Long tenantId, Long storeId, Long orderId);
}
