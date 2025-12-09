package com.bluecone.app.payment.simple.application;

import com.bluecone.app.core.event.DomainEventPublisher;
import com.bluecone.app.payment.domain.event.PaymentSuccessEvent;
import com.bluecone.app.payment.simple.application.dto.PaymentOrderDTO;
import com.bluecone.app.payment.simple.domain.enums.PaymentStatus;
import com.bluecone.app.payment.simple.domain.model.PaymentOrder;
import com.bluecone.app.payment.simple.domain.repository.SimplePaymentOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentCommandAppService {

    private final SimplePaymentOrderRepository repository;
    private final DomainEventPublisher eventPublisher;

    public PaymentCommandAppService(SimplePaymentOrderRepository repository,
                                    DomainEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
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

        PaymentOrderDTO dto = PaymentOrderDTO.from(paymentOrder);
        PaymentSuccessEvent event = new PaymentSuccessEvent(
                paymentOrder.getTenantId(),
                paymentOrder.getStoreId(),
                paymentOrder.getUserId(),
                paymentOrder.getOrderId(),
                paymentOrder.getId(),
                paymentOrder.getTotalAmount(),
                paymentOrder.getPaidAmount(),
                paymentOrder.getChannel() != null ? paymentOrder.getChannel().name() : null,
                paymentOrder.getOutTransactionNo()
        );
        eventPublisher.publish(event);
        return dto;
    }
}
