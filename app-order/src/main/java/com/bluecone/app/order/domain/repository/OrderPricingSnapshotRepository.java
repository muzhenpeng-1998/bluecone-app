package com.bluecone.app.order.domain.repository;

import com.bluecone.app.order.domain.model.OrderPricingSnapshot;

/**
 * 订单计价快照仓储接口
 */
public interface OrderPricingSnapshotRepository {
    
    /**
     * 保存计价快照
     * 
     * @param snapshot 计价快照
     * @return 保存后的快照（包含ID）
     */
    OrderPricingSnapshot save(OrderPricingSnapshot snapshot);
    
    /**
     * 根据订单ID查询计价快照
     * 
     * @param orderId 订单ID
     * @return 计价快照，不存在返回null
     */
    OrderPricingSnapshot findByOrderId(Long orderId);
    
    /**
     * 根据报价单ID查询计价快照
     * 
     * @param quoteId 报价单ID
     * @return 计价快照，不存在返回null
     */
    OrderPricingSnapshot findByQuoteId(String quoteId);
}
