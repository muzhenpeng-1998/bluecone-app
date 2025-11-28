package com.bluecone.app.order.service;

import com.bluecone.app.core.config.Feature;
import com.bluecone.app.core.config.FeatureGate;
import com.bluecone.app.core.config.domain.OrderConfig;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Example order service demonstrating consumption of domain configs and feature gate.
 */
@Service
public class ConfigDrivenOrderService {

    private final OrderConfig orderConfig;
    private final FeatureGate featureGate;

    public ConfigDrivenOrderService(OrderConfig orderConfig, FeatureGate featureGate) {
        this.orderConfig = orderConfig;
        this.featureGate = featureGate;
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
}
