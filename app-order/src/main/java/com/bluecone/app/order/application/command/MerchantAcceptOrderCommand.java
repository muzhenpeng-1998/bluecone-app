package com.bluecone.app.order.application.command;

import com.bluecone.app.order.api.dto.MerchantAcceptOrderRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商户接单命令，承载租户/门店/操作人/订单等必要信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantAcceptOrderCommand {

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

    /**
     * 从请求对象构建命令。
     */
    public static MerchantAcceptOrderCommand fromRequest(MerchantAcceptOrderRequest request, Long orderId) {
        return MerchantAcceptOrderCommand.builder()
                .tenantId(request.getTenantId())
                .storeId(request.getStoreId())
                .operatorId(request.getOperatorId())
                .orderId(orderId)
                .requestId(request.getRequestId())
                .expectedVersion(request.getExpectedVersion())
                .build();
    }
}
