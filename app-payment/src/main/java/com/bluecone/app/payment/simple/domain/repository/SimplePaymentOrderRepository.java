package com.bluecone.app.payment.simple.domain.repository;

import com.bluecone.app.payment.simple.domain.model.PaymentOrder;
import java.util.Optional;

/**
 * 简化支付单仓储接口。
 */
public interface SimplePaymentOrderRepository {

    PaymentOrder save(PaymentOrder order);

    Optional<PaymentOrder> findById(Long tenantId, Long payOrderId);

    Optional<PaymentOrder> findByOrderId(Long tenantId, Long orderId);
}
