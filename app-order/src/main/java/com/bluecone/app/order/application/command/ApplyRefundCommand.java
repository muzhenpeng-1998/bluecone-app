package com.bluecone.app.order.application.command;

import com.bluecone.app.order.domain.enums.RefundChannel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 申请退款命令。
 * 
 * <h3>命令职责：</h3>
 * <ul>
 *   <li>封装申请退款的请求参数</li>
 *   <li>支持幂等性（requestId）</li>
 *   <li>支持并发控制（expectedVersion）</li>
 *   <li>记录退款原因（reasonCode、reasonDesc）</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyRefundCommand {
    
    /**
     * 租户ID。
     */
    private Long tenantId;
    
    /**
     * 门店ID。
     */
    private Long storeId;
    
    /**
     * 订单ID。
     */
    private Long orderId;
    
    /**
     * 订单号（冗余，用于快速查询）。
     */
    private String publicOrderNo;
    
    /**
     * 请求ID（幂等键）。
     */
    private String requestId;
    
    /**
     * 期望版本号（乐观锁）。
     */
    private Integer expectedVersion;
    
    /**
     * 退款渠道（WECHAT、ALIPAY、MOCK）。
     */
    private RefundChannel channel;
    
    /**
     * 退款金额（实际退款金额，单位：元）。
     */
    private BigDecimal refundAmount;
    
    /**
     * 退款原因码（USER_CANCEL、MERCHANT_REJECT、OUT_OF_STOCK等）。
     */
    private String reasonCode;
    
    /**
     * 退款原因描述（用户或商户填写的具体原因）。
     */
    private String reasonDesc;
    
    /**
     * 支付单ID（关联bc_payment_order.id）。
     */
    private Long payOrderId;
    
    /**
     * 第三方支付单号（如微信transaction_id，用于退款时传给支付网关）。
     */
    private String payNo;
}
