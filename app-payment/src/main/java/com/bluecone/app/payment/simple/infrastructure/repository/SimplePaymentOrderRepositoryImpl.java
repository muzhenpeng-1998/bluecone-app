package com.bluecone.app.payment.simple.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.payment.simple.domain.model.PaymentOrder;
import com.bluecone.app.payment.simple.domain.repository.SimplePaymentOrderRepository;
import com.bluecone.app.payment.simple.infrastructure.converter.PaymentOrderConverter;
import com.bluecone.app.payment.simple.infrastructure.mapper.SimplePaymentOrderMapper;
import com.bluecone.app.payment.simple.infrastructure.persistence.PaymentOrderDO;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class SimplePaymentOrderRepositoryImpl implements SimplePaymentOrderRepository {

    private final SimplePaymentOrderMapper paymentOrderMapper;

    public SimplePaymentOrderRepositoryImpl(SimplePaymentOrderMapper paymentOrderMapper) {
        this.paymentOrderMapper = paymentOrderMapper;
    }

    @Override
    public PaymentOrder save(PaymentOrder order) {
        if (order == null) {
            return null;
        }
        PaymentOrderDO doObj = PaymentOrderConverter.toDO(order);
        boolean insert = order.getId() == null;
        LocalDateTime now = LocalDateTime.now();
        doObj.setUpdatedAt(now);
        if (insert) {
            doObj.setCreatedAt(now);
            paymentOrderMapper.insert(doObj);
        } else {
            paymentOrderMapper.updateById(doObj);
        }
        order.setId(doObj.getId());
        order.setUpdatedAt(doObj.getUpdatedAt());
        order.setCreatedAt(doObj.getCreatedAt());
        return order;
    }

    @Override
    public Optional<PaymentOrder> findById(Long tenantId, Long payOrderId) {
        if (tenantId == null || payOrderId == null) {
            return Optional.empty();
        }
        PaymentOrderDO doObj = paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrderDO>()
                .eq(PaymentOrderDO::getId, payOrderId)
                .eq(PaymentOrderDO::getTenantId, tenantId));
        return Optional.ofNullable(PaymentOrderConverter.toDomain(doObj));
    }

    @Override
    public Optional<PaymentOrder> findByOrderId(Long tenantId, Long orderId) {
        if (tenantId == null || orderId == null) {
            return Optional.empty();
        }
        PaymentOrderDO doObj = paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrderDO>()
                .eq(PaymentOrderDO::getTenantId, tenantId)
                .eq(PaymentOrderDO::getOrderId, orderId));
        return Optional.ofNullable(PaymentOrderConverter.toDomain(doObj));
    }
}
