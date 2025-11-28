package com.bluecone.app.core.config.domain;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Payment domain configuration contract exposed to services.
 */
public interface PaymentConfig {

    Duration paymentExpireAfter();

    boolean allowPartialRefund();

    BigDecimal maxRefundAmountPerDay();
}
