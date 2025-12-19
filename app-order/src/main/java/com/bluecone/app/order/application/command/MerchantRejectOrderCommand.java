package com.bluecone.app.order.application.command;

import com.bluecone.app.order.api.dto.MerchantRejectOrderRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商户拒单命令，承载租户/门店/操作人/订单/拒单原因等必要信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantRejectOrderCommand {

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
     * 拒单原因码（如：OUT_OF_STOCK、BUSY、OTHER）。
     */
    private String reasonCode;

    /**
     * 拒单原因说明（商户填写的具体原因）。
     */
    private String reasonDesc;

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
    public static MerchantRejectOrderCommand fromRequest(MerchantRejectOrderRequest request, Long orderId) {
        return MerchantRejectOrderCommand.builder()
                .tenantId(request.getTenantId())
                .storeId(request.getStoreId())
                .operatorId(request.getOperatorId())
                .orderId(orderId)
                .reasonCode(request.getReasonCode())
                .reasonDesc(request.getReasonDesc())
                .requestId(request.getRequestId())
                .expectedVersion(request.getExpectedVersion())
                .build();
    }
}
