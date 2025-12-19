package com.bluecone.app.order.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单完成命令，承载租户/门店/操作人/订单等必要信息。
 * 
 * <h3>用途：</h3>
 * <p>用户取餐/配送完成后，点击"完成订单"按钮时触发，状态流转：READY → COMPLETED。</p>
 * 
 * <h3>幂等性：</h3>
 * <p>通过 requestId 保证幂等，同一 requestId 重复调用返回已有结果，不产生副作用。</p>
 * 
 * <h3>并发保护：</h3>
 * <p>通过 expectedVersion（乐观锁）防止并发冲突，确保状态流转正确。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteOrderCommand {

    /**
     * 租户ID。
     */
    private Long tenantId;

    /**
     * 门店ID。
     */
    private Long storeId;

    /**
     * 操作人ID。
     */
    private Long operatorId;

    /**
     * 订单ID。
     */
    private Long orderId;

    /**
     * 请求ID（用于幂等）。
     */
    private String requestId;

    /**
     * 期望的订单版本号（用于乐观锁）。
     */
    private Integer expectedVersion;
}
