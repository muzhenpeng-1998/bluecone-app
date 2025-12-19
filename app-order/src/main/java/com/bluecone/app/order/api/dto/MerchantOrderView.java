package com.bluecone.app.order.api.dto;

import com.bluecone.app.order.domain.model.Order;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 商户侧订单视图，主要展示订单号、状态、接单/拒单记录。
 */
@Data
public class MerchantOrderView {

    private Long orderId;

    private String orderNo;

    private String status;

    private String payStatus;

    private BigDecimal payableAmount;

    private Integer version;

    private Long acceptOperatorId;

    private LocalDateTime acceptedAt;

    private String rejectReasonCode;

    private String rejectReasonDesc;

    private LocalDateTime rejectedAt;

    private Long rejectedBy;

    public static MerchantOrderView from(Order order) {
        if (order == null) {
            return null;
        }
        MerchantOrderView view = new MerchantOrderView();
        view.setOrderId(order.getId());
        view.setOrderNo(order.getOrderNo());
        view.setStatus(order.getStatus() != null ? order.getStatus().getCode() : null);
        view.setPayStatus(order.getPayStatus() != null ? order.getPayStatus().getCode() : null);
        view.setPayableAmount(order.getPayableAmount());
        view.setVersion(order.getVersion());
        view.setAcceptOperatorId(order.getAcceptOperatorId());
        view.setAcceptedAt(order.getAcceptedAt());
        view.setRejectReasonCode(order.getRejectReasonCode());
        view.setRejectReasonDesc(order.getRejectReasonDesc());
        view.setRejectedAt(order.getRejectedAt());
        view.setRejectedBy(order.getRejectedBy());
        return view;
    }
}
