package com.bluecone.app.order.domain.repository;

import com.bluecone.app.order.domain.model.RefundOrder;

/**
 * 退款单仓储接口。
 * 
 * <h3>职责：</h3>
 * <ul>
 *   <li>退款单的持久化存储与查询</li>
 *   <li>支持根据幂等键（idemKey）查询，保证幂等性</li>
 *   <li>支持根据订单ID查询退款单列表</li>
 * </ul>
 */
public interface RefundOrderRepository {
    
    /**
     * 根据租户和退款单ID查询退款单，不存在返回 null。
     * 
     * @param tenantId 租户ID
     * @param refundOrderId 退款单ID
     * @return 退款单聚合根，不存在返回 null
     */
    RefundOrder findById(Long tenantId, Long refundOrderId);
    
    /**
     * 根据租户和幂等键查询退款单，用于幂等性保护。
     * 
     * @param tenantId 租户ID
     * @param idemKey 幂等键（格式：{tenantId}:{storeId}:{orderId}:refund:{requestId}）
     * @return 退款单聚合根，不存在返回 null
     */
    RefundOrder findByIdemKey(Long tenantId, String idemKey);
    
    /**
     * 根据租户和订单ID查询最近一笔退款单（用于取消订单后查询退款状态）。
     * 
     * @param tenantId 租户ID
     * @param orderId 订单ID
     * @return 退款单聚合根，不存在返回 null
     */
    RefundOrder findLatestByOrderId(Long tenantId, Long orderId);
    
    /**
     * 新建退款单。
     * 
     * @param refundOrder 退款单聚合根
     */
    void save(RefundOrder refundOrder);
    
    /**
     * 更新退款单（使用乐观锁）。
     * 
     * @param refundOrder 退款单聚合根
     * @return 更新行数（用于判断乐观锁是否成功）
     */
    int update(RefundOrder refundOrder);
}
