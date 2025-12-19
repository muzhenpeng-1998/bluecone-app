package com.bluecone.app.order.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 订单定价上下文
 * 
 * <p>包含订单定价所需的所有信息，用于：
 * <ul>
 *   <li>计算商品总价</li>
 *   <li>查询可用优惠券</li>
 *   <li>计算优惠金额</li>
 *   <li>计算应付金额</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingContext implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 门店ID
     */
    private Long storeId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 订单明细列表
     */
    private List<OrderItem> items;

    /**
     * 指定使用的优惠券ID（可选）
     */
    private Long couponId;

    /**
     * 订单类型
     */
    private String orderType;

    /**
     * 渠道
     */
    private String channel;
}
