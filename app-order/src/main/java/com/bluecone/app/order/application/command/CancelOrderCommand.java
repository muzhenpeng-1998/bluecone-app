package com.bluecone.app.order.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 取消订单命令。
 * 
 * <h3>命令职责：</h3>
 * <ul>
 *   <li>封装取消订单的请求参数</li>
 *   <li>支持幂等性（requestId）</li>
 *   <li>支持并发控制（expectedVersion）</li>
 *   <li>记录取消原因（reasonCode、reasonDesc）</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelOrderCommand {
    
    /**
     * 租户ID。
     */
    private Long tenantId;
    
    /**
     * 门店ID。
     */
    private Long storeId;
    
    /**
     * 用户ID（用户取消时必填）。
     */
    private Long userId;
    
    /**
     * 订单ID。
     */
    private Long orderId;
    
    /**
     * 请求ID（幂等键）。
     */
    private String requestId;
    
    /**
     * 期望版本号（乐观锁）。
     */
    private Integer expectedVersion;
    
    /**
     * 取消原因码（USER_CANCEL、MERCHANT_REJECT、PAY_TIMEOUT等）。
     */
    private String reasonCode;
    
    /**
     * 取消原因描述（用户或商户填写的具体原因）。
     */
    private String reasonDesc;
}
