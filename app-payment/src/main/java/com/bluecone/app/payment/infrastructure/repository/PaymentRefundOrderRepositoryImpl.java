package com.bluecone.app.payment.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.payment.domain.model.PaymentRefundOrder;
import com.bluecone.app.payment.domain.repository.PaymentRefundOrderRepository;
import com.bluecone.app.payment.infrastructure.persistence.PaymentRefundDO;
import com.bluecone.app.payment.infrastructure.persistence.PaymentRefundMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class PaymentRefundOrderRepositoryImpl implements PaymentRefundOrderRepository {

    private final PaymentRefundMapper mapper;

    public PaymentRefundOrderRepositoryImpl(PaymentRefundMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<PaymentRefundOrder> findByRefundDate(LocalDate refundDate) {
        LambdaQueryWrapper<PaymentRefundDO> wrapper = new LambdaQueryWrapper<>();
        if (refundDate != null) {
            wrapper.ge(PaymentRefundDO::getUpdatedAt, refundDate.atStartOfDay())
                    .lt(PaymentRefundDO::getUpdatedAt, refundDate.plusDays(1).atStartOfDay());
        }
        return mapper.selectList(wrapper).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<PaymentRefundOrder> findSucceededByRefundDate(LocalDate refundDate) {
        LambdaQueryWrapper<PaymentRefundDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentRefundDO::getStatus, "SUCCESS");
        if (refundDate != null) {
            wrapper.ge(PaymentRefundDO::getUpdatedAt, refundDate.atStartOfDay())
                    .lt(PaymentRefundDO::getUpdatedAt, refundDate.plusDays(1).atStartOfDay());
        }
        return mapper.selectList(wrapper).stream().map(this::toDomain).collect(Collectors.toList());
    }

    private PaymentRefundOrder toDomain(PaymentRefundDO doObj) {
        if (doObj == null) {
            return null;
        }
        PaymentRefundOrder order = new PaymentRefundOrder();
        order.setId(doObj.getId());
        order.setTenantId(doObj.getTenantId());
        order.setPaymentOrderId(doObj.getPaymentOrderId());
        order.setRefundNo(doObj.getRefundNo());
        order.setThirdRefundNo(doObj.getThirdRefundNo());
        order.setRefundAmount(doObj.getRefundAmount());
        order.setStatus(doObj.getStatus());
        order.setCreatedAt(doObj.getCreatedAt());
        order.setUpdatedAt(doObj.getUpdatedAt());
        return order;
    }
}
