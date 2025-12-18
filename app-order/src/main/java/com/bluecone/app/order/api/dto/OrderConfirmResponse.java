package com.bluecone.app.order.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单确认单响应（M0）。
 * <p>包含价格计算结果、门店可接单状态、confirmToken 等信息。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderConfirmResponse {

    /**
     * 确认令牌（用于后续提交单校验，防止价格篡改）。
     * <p>格式：SHA-256(tenantId + storeId + userId + items + timestamp)</p>
     */
    private String confirmToken;

    /**
     * 价格版本号（时间戳，用于校验价格是否过期）。
     */
    private Long priceVersion;

    /**
     * 订单总金额（原价总额）。
     */
    private BigDecimal totalAmount;

    /**
     * 优惠金额（M0暂时为0，预留）。
     */
    private BigDecimal discountAmount;

    /**
     * 应付金额（实际支付金额）。
     */
    private BigDecimal payableAmount;

    /**
     * 币种（默认CNY）。
     */
    private String currency;

    /**
     * 订单明细列表（包含价格快照）。
     */
    private List<OrderConfirmItemResponse> items;

    /**
     * 门店是否可接单。
     */
    private Boolean storeAcceptable;

    /**
     * 门店不可接单原因码（如：STORE_CLOSED、OUT_OF_BUSINESS_HOURS）。
     */
    private String storeRejectReasonCode;

    /**
     * 门店不可接单原因消息（中文提示）。
     */
    private String storeRejectReasonMessage;

    /**
     * 失败原因列表（如商品不存在、库存不足等）。
     */
    private List<String> failureReasons;
}
