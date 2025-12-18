package com.bluecone.app.order.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 订单提交单请求（M0）。
 * <p>用户侧调用，用于正式创建订单并落库，必须携带 confirmToken 和 clientRequestId（幂等键）。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSubmitRequest {

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
     * 确认令牌（必填，来自 confirm 接口返回）。
     */
    private String confirmToken;

    /**
     * 价格版本号（必填，来自 confirm 接口返回）。
     */
    private Long priceVersion;

    /**
     * 客户端请求ID（必填，用于幂等）。
     * <p>规则：前端生成UUID或时间戳，同一个 clientRequestId 只会创建一次订单。</p>
     */
    private String clientRequestId;

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
}
