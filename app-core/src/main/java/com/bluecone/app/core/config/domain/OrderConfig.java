package com.bluecone.app.core.config.domain;

import java.time.Duration;

/**
 * Order domain configuration contract exposed to services.
 */
public interface OrderConfig {

    boolean isNewEngineEnabled();

    Duration paymentTimeout();

    int maxItemsPerOrder();

    boolean allowCrossDayOrder();
}
