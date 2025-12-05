package com.bluecone.app.order.application.assembler;

import com.bluecone.app.order.api.dto.ConfirmOrderResponse;
import com.bluecone.app.order.domain.model.Order;
import java.util.Collections;
import java.util.Map;

public class OrderAppAssembler {

    private OrderAppAssembler() {
    }

    public static ConfirmOrderResponse toConfirmResponse(Order order) {
        ConfirmOrderResponse resp = new ConfirmOrderResponse();
        resp.setOrderId(order.getId());
        resp.setOrderNo(order.getOrderNo());
        resp.setStatus(order.getStatus() != null ? order.getStatus().getCode() : null);
        resp.setPayStatus(order.getPayStatus() != null ? order.getPayStatus().getCode() : null);
        resp.setPayableAmount(order.getPayableAmount());
        resp.setCurrency(order.getCurrency());
        resp.setPayChannel(null);
        boolean needPay = order.getPayableAmount() != null && order.getPayableAmount().signum() > 0;
        resp.setNeedPay(needPay);
        resp.setPaymentTimeoutSeconds(900);
        resp.setPaymentPayload(Collections.emptyMap());
        resp.setExt(toSafeMap(order.getExt()));
        return resp;
    }

    private static Map<String, Object> toSafeMap(Map<String, Object> source) {
        return source == null ? Collections.emptyMap() : source;
    }
}
