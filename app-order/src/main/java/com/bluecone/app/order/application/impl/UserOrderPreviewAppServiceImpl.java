package com.bluecone.app.order.application.impl;

import com.bluecone.app.order.api.dto.ConfirmOrderPreviewRequest;
import com.bluecone.app.order.api.dto.ConfirmOrderPreviewResponse;
import com.bluecone.app.order.api.dto.ConfirmOrderRequest;
import com.bluecone.app.order.application.UserOrderPreviewAppService;
import com.bluecone.app.order.domain.model.Order;
import com.bluecone.app.order.domain.service.OrderDomainService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserOrderPreviewAppServiceImpl implements UserOrderPreviewAppService {

    private final OrderDomainService orderDomainService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ConfirmOrderPreviewResponse preview(ConfirmOrderPreviewRequest previewRequest) {
        ConfirmOrderRequest confirmRequest = new ConfirmOrderRequest();
        confirmRequest.setTenantId(previewRequest.getTenantId());
        confirmRequest.setStoreId(previewRequest.getStoreId());
        confirmRequest.setUserId(previewRequest.getUserId());
        confirmRequest.setBizType(previewRequest.getBizType());
        confirmRequest.setOrderSource(previewRequest.getOrderSource());
        confirmRequest.setChannel(previewRequest.getChannel());
        confirmRequest.setItems(previewRequest.getItems());
        confirmRequest.setClientTotalAmount(previewRequest.getClientPayableAmount() != null
                ? previewRequest.getClientPayableAmount()
                : java.math.BigDecimal.ZERO);
        confirmRequest.setClientDiscountAmount(java.math.BigDecimal.ZERO);
        confirmRequest.setClientPayableAmount(previewRequest.getClientPayableAmount() != null
                ? previewRequest.getClientPayableAmount()
                : java.math.BigDecimal.ZERO);
        confirmRequest.setSessionId(previewRequest.getSessionId());
        confirmRequest.setSessionVersion(previewRequest.getSessionVersion());
        confirmRequest.setRemark(previewRequest.getRemark());
        confirmRequest.setExt(parseExt(previewRequest.getExt()));
        confirmRequest.setClientOrderNo("PREVIEW-" + Instant.now().toEpochMilli());
        confirmRequest.setAutoCreatePayment(false);
        confirmRequest.setPayChannel(null);

        Order order = orderDomainService.buildConfirmedOrder(confirmRequest);

        ConfirmOrderPreviewResponse resp = new ConfirmOrderPreviewResponse();
        resp.setCanPlaceOrder(true);
        resp.setTotalAmount(order.getTotalAmount());
        resp.setDiscountAmount(order.getDiscountAmount());
        resp.setPayableAmount(order.getPayableAmount());
        resp.setCurrency(order.getCurrency());
        resp.setExpectedReadyTimeSeconds(600);
        resp.setStoreOpenStatus("UNKNOWN");
        resp.setMessage(null);
        resp.setSessionVersion(order.getSessionVersion());
        resp.setExt(previewRequest.getExt());
        return resp;
    }

    private Map<String, Object> parseExt(String ext) {
        if (ext == null || ext.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(ext, new TypeReference<>() {
            });
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}
