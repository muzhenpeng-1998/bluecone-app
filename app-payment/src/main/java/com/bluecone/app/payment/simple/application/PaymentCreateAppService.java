package com.bluecone.app.payment.simple.application;

import com.bluecone.app.payment.simple.application.dto.PaymentOrderDTO;
import com.bluecone.app.payment.simple.domain.model.PaymentOrder;
import com.bluecone.app.payment.simple.domain.repository.SimplePaymentOrderRepository;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * 创建调试支付单的应用服务。
 */
@Service
public class PaymentCreateAppService {

    private final SimplePaymentOrderRepository repository;

    public PaymentCreateAppService(SimplePaymentOrderRepository repository) {
        this.repository = repository;
    }

    public PaymentOrderDTO createForOrder(Long tenantId,
                                          Long storeId,
                                          Long userId,
                                          Long orderId,
                                          Long totalAmount) {
        if (Objects.requireNonNull(totalAmount, "totalAmount") < 0) {
            throw new IllegalArgumentException("totalAmount 不能为空且不能为负");
        }
        PaymentOrder paymentOrder = PaymentOrder.createForOrder(tenantId, storeId, userId, orderId, totalAmount);
        repository.save(paymentOrder);
        return PaymentOrderDTO.from(paymentOrder);
    }
}
