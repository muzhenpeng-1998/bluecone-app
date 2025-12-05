package com.bluecone.app.order.service;

import com.bluecone.app.core.config.Feature;
import com.bluecone.app.core.config.FeatureGate;
import com.bluecone.app.core.config.domain.OrderConfig;
import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.order.api.dto.ConfirmOrderRequest;
import com.bluecone.app.order.api.dto.ConfirmOrderResponse;
import com.bluecone.app.order.application.OrderConfirmAppService;
import com.bluecone.app.order.application.command.ConfirmOrderCommand;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

/**
 * Example order service demonstrating consumption of domain configs and feature gate.
 */
@Service
public class ConfigDrivenOrderService {

    private final OrderConfig orderConfig;
    private final FeatureGate featureGate;
    private final OrderConfirmAppService orderConfirmAppService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ConfigDrivenOrderService(OrderConfig orderConfig,
                                    FeatureGate featureGate,
                                    OrderConfirmAppService orderConfirmAppService) {
        this.orderConfig = orderConfig;
        this.featureGate = featureGate;
        this.orderConfirmAppService = orderConfirmAppService;
    }

    public void createOrder() {
        if (orderConfig.isNewEngineEnabled()) {
            // invoke new order engine
        } else {
            // fallback to legacy engine
        }

        Duration timeout = orderConfig.paymentTimeout();
        int maxItems = orderConfig.maxItemsPerOrder();
        boolean allowCrossDay = orderConfig.allowCrossDayOrder();
        // use timeout/maxItems/allowCrossDay to steer downstream logic
    }

    public void createOrderWithFeature() {
        if (featureGate.isOn(Feature.NEW_ORDER_ENGINE)) {
            // new engine
        } else {
            // old engine
        }
    }

    /**
     * 确认订单入口，负责调用应用服务完成幂等校验、订单生成与落库。
     */
    public ConfirmOrderResponse confirmOrder(ConfirmOrderCommand command) {
        if (command == null || command.getTenantId() == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "缺少租户标识");
        }
        ConfirmOrderRequest req = new ConfirmOrderRequest();
        req.setTenantId(command.getTenantId());
        req.setStoreId(command.getStoreId());
        req.setUserId(command.getUserId());
        req.setSessionId(command.getSessionId());
        req.setSessionVersion(command.getSessionVersion());
        req.setClientOrderNo(command.getClientOrderNo());
        req.setOrderSource(command.getOrderSource());
        req.setBizType(command.getBizType());
        req.setChannel(command.getChannel());
        req.setItems(command.getItems());
        req.setClientTotalAmount(command.getClientTotalAmount());
        req.setClientDiscountAmount(command.getClientDiscountAmount());
        req.setClientPayableAmount(command.getClientPayableAmount());
        req.setRemark(command.getRemark());
        req.setPayChannel(command.getPayChannel());
        req.setAutoCreatePayment(true);
        req.setExt(parseExt(command.getExtJson()));
        return orderConfirmAppService.confirmOrder(req);
    }

    private Map<String, Object> parseExt(String extJson) {
        if (extJson == null || extJson.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(extJson, new TypeReference<>() {
            });
        } catch (IOException e) {
            return Collections.emptyMap();
        }
    }
}
