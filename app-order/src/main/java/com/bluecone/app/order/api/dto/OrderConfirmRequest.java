package com.bluecone.app.order.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单确认单请求（M0）。
 * <p>用户侧调用，用于预校验门店可接单、商品有效性、计算价格等，返回 confirmToken 供后续提交使用。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderConfirmRequest {

    /**
     * 租户ID（必填）。
     */
    private Long tenantId;

    /**
     * 门店ID（必填）。
     */
    private Long storeId;

    /**
     * 用户ID（必填）。
     */
    private Long userId;

    /**
     * 订单明细列表（必填，至少一项）。
     */
    private List<OrderConfirmItemRequest> items;

    /**
     * 配送类型（必填）：DINE_IN（堂食）、TAKEAWAY（外卖）、PICKUP（自取）。
     */
    private String deliveryType;

    /**
     * 渠道标识（可选）：MINI_PROGRAM、H5、POS 等。
     */
    private String channel;

    /**
     * 订单来源（可选）：MINI_PROGRAM、H5、POS 等。
     */
    private String orderSource;

    /**
     * 用户备注（可选）。
     */
    private String remark;
    
    /**
     * 会员ID（可选）
     */
    private Long memberId;
    
    /**
     * 优惠券ID（可选）
     */
    private Long couponId;
    
    /**
     * 使用积分数量（可选）
     */
    private Integer usePoints;
    
    /**
     * 配送距离（公里，可选）
     */
    private BigDecimal deliveryDistance;
    
    /**
     * 订单类型（可选）
     */
    private String orderType;
    
    /**
     * 是否启用抹零（可选）
     */
    private Boolean enableRounding;
}
