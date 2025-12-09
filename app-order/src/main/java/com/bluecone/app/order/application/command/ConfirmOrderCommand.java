package com.bluecone.app.order.application.command;

import com.bluecone.app.order.api.dto.ConfirmOrderItemDTO;
import com.bluecone.app.order.api.dto.ConfirmOrderRequest;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 确认订单命令，承载上下文与幂等信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmOrderCommand {

    private Long tenantId;

    private Long storeId;

    private Long userId;

    private Long orderId;

    private String clientOrderNo;

    private String sessionId;

    private Integer sessionVersion;

    private Long version;

    private String payChannel;

    private BigDecimal payAmount;

    private String currency;

    private String remark;

    private String extJson;

    private String channel;

    private String bizType;

    private String orderSource;

    private BigDecimal clientTotalAmount;

    private BigDecimal clientDiscountAmount;

    private BigDecimal clientPayableAmount;

    private List<ConfirmOrderItemDTO> items;

    public static ConfirmOrderCommand fromRequest(ConfirmOrderRequest request,
                                                  Long tenantId,
                                                  Long storeId,
                                                  Long userId) {
        // TODO: 后续应从登录态/上下文读取 tenantId/storeId/userId，当前阶段允许从请求体携带。
        return ConfirmOrderCommand.builder()
                .tenantId(tenantId)
                .storeId(storeId != null ? storeId : request.getStoreId())
                .userId(userId != null ? userId : request.getUserId())
                .clientOrderNo(request.getClientOrderNo())
                .sessionId(request.getSessionId())
                .sessionVersion(request.getSessionVersion())
                .payChannel(request.getPayChannel())
                .currency("CNY")
                .remark(request.getRemark())
                .channel(request.getChannel())
                .bizType(request.getBizType())
                .orderSource(request.getOrderSource())
                .clientTotalAmount(request.getClientTotalAmount())
                .clientDiscountAmount(request.getClientDiscountAmount())
                .clientPayableAmount(request.getClientPayableAmount())
                .items(request.getItems())
                .build();
    }
}
