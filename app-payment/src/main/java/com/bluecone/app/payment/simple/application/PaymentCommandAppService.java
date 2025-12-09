package com.bluecone.app.payment.simple.application;

import com.bluecone.app.payment.simple.application.dto.PaymentOrderDTO;
import com.bluecone.app.payment.simple.domain.enums.PaymentStatus;
import com.bluecone.app.payment.simple.domain.model.PaymentOrder;
import com.bluecone.app.payment.simple.domain.repository.SimplePaymentOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentCommandAppService {

    private final SimplePaymentOrderRepository repository;

    public PaymentCommandAppService(SimplePaymentOrderRepository repository) {
        this.repository = repository;
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentOrderDTO markPaid(Long tenantId, Long payOrderId) {
        PaymentOrder paymentOrder = repository.findById(tenantId, payOrderId)
                .orElseThrow(() -> new IllegalStateException("支付单不存在"));
        if (paymentOrder.getStatus() == PaymentStatus.SUCCESS) {
            return PaymentOrderDTO.from(paymentOrder);
        }
        if (paymentOrder.getStatus() != PaymentStatus.WAIT_PAY) {
            throw new IllegalStateException("支付单状态不允许标记成功");
        }
        Long totalAmount = paymentOrder.getTotalAmount() == null ? 0L : paymentOrder.getTotalAmount();
        paymentOrder.markSuccess(totalAmount, "debug-" + payOrderId);
        repository.save(paymentOrder);
        return PaymentOrderDTO.from(paymentOrder);
    }
}
