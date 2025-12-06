package com.bluecone.app.payment.domain.repository;

import com.bluecone.app.payment.domain.model.PaymentRefundOrder;
import java.time.LocalDate;
import java.util.List;

public interface PaymentRefundOrderRepository {

    List<PaymentRefundOrder> findByRefundDate(LocalDate refundDate);

    List<PaymentRefundOrder> findSucceededByRefundDate(LocalDate refundDate);
}
