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

    private Long tenantId;

    private Long storeId;

    private Long operatorId;

    private Long orderId;

    public static MerchantAcceptOrderCommand fromRequest(MerchantAcceptOrderRequest request, Long orderId) {
        return MerchantAcceptOrderCommand.builder()
                .tenantId(request.getTenantId())
                .storeId(request.getStoreId())
                .operatorId(request.getOperatorId())
                .orderId(orderId)
                .build();
    }
}
