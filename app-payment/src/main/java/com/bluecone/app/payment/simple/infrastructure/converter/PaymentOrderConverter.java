package com.bluecone.app.payment.simple.infrastructure.converter;

import com.bluecone.app.payment.simple.domain.enums.PaymentChannel;
import com.bluecone.app.payment.simple.domain.enums.PaymentStatus;
import com.bluecone.app.payment.simple.domain.model.PaymentOrder;
import com.bluecone.app.payment.simple.infrastructure.persistence.PaymentOrderDO;
import java.time.LocalDateTime;
import org.springframework.util.StringUtils;

/**
 * 简单支付单聚合与 DO 之间的转换器。
 */
public class PaymentOrderConverter {

    private PaymentOrderConverter() {
    }

    public static PaymentOrder toDomain(PaymentOrderDO doObj) {
        if (doObj == null) {
            return null;
        }
        PaymentOrder order = new PaymentOrder();
        order.setId(doObj.getId());
        order.setTenantId(doObj.getTenantId());
        order.setStoreId(doObj.getStoreId());
        order.setUserId(doObj.getUserId());
        order.setOrderId(doObj.getOrderId());
        order.setPayOrderNo(doObj.getPayOrderNo());
        order.setTotalAmount(doObj.getTotalAmount());
        order.setPaidAmount(doObj.getPaidAmount());
        order.setChannel(parseChannel(doObj.getChannel()));
        order.setStatus(parseStatus(doObj.getStatus()));
        order.setOutTransactionNo(doObj.getOutTransactionNo());
        order.setCreatedAt(doObj.getCreatedAt());
        order.setPaidAt(doObj.getPaidAt());
        order.setUpdatedAt(doObj.getUpdatedAt());
        return order;
    }

    public static PaymentOrderDO toDO(PaymentOrder order) {
        if (order == null) {
            return null;
        }
        PaymentOrderDO doObj = new PaymentOrderDO();
        doObj.setId(order.getId());
        doObj.setTenantId(order.getTenantId());
        doObj.setStoreId(order.getStoreId());
        doObj.setUserId(order.getUserId());
        doObj.setOrderId(order.getOrderId());
        doObj.setPayOrderNo(order.getPayOrderNo());
        doObj.setTotalAmount(order.getTotalAmount());
        doObj.setPaidAmount(order.getPaidAmount());
        doObj.setChannel(order.getChannel() != null ? order.getChannel().getCode() : null);
        doObj.setStatus(order.getStatus() != null ? order.getStatus().getCode() : null);
        doObj.setOutTransactionNo(order.getOutTransactionNo());
        doObj.setCreatedAt(order.getCreatedAt());
        doObj.setPaidAt(order.getPaidAt());
        doObj.setUpdatedAt(order.getUpdatedAt());
        return doObj;
    }

    private static PaymentChannel parseChannel(String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        for (PaymentChannel channel : PaymentChannel.values()) {
            if (channel.getCode().equalsIgnoreCase(code)) {
                return channel;
            }
        }
        return null;
    }

    private static PaymentStatus parseStatus(String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        for (PaymentStatus status : PaymentStatus.values()) {
            if (status.getCode().equalsIgnoreCase(code)) {
                return status;
            }
        }
        return null;
    }
}
