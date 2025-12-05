package com.bluecone.app.order.infra.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bluecone.app.order.domain.model.OrderPayment;
import com.bluecone.app.order.domain.repository.OrderPaymentRepository;
import com.bluecone.app.order.infra.persistence.converter.OrderConverter;
import com.bluecone.app.order.infra.persistence.mapper.OrderPaymentMapper;
import com.bluecone.app.order.infra.persistence.po.OrderPaymentPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OrderPaymentRepositoryImpl implements OrderPaymentRepository {

    private final OrderPaymentMapper orderPaymentMapper;

    @Override
    public void save(OrderPayment payment) {
        if (payment == null) {
            return;
        }
        OrderPaymentPO po = OrderConverter.toPaymentPO(payment);
        orderPaymentMapper.insert(po);
    }

    @Override
    public OrderPayment findByOrderId(Long tenantId, Long orderId) {
        OrderPaymentPO po = orderPaymentMapper.selectOne(new LambdaQueryWrapper<OrderPaymentPO>()
                .eq(OrderPaymentPO::getTenantId, tenantId)
                .eq(OrderPaymentPO::getOrderId, orderId));
        return OrderConverter.toDomain(po);
    }

    @Override
    public OrderPayment findByChannelAndTradeNo(Long tenantId, String payChannel, String thirdTradeNo) {
        OrderPaymentPO po = orderPaymentMapper.selectOne(new LambdaQueryWrapper<OrderPaymentPO>()
                .eq(OrderPaymentPO::getTenantId, tenantId)
                .eq(OrderPaymentPO::getPayChannel, payChannel)
                .eq(OrderPaymentPO::getThirdTradeNo, thirdTradeNo));
        return OrderConverter.toDomain(po);
    }

    @Override
    public void updateStatus(OrderPayment payment) {
        if (payment == null || payment.getId() == null || payment.getTenantId() == null) {
            return;
        }
        OrderPaymentPO po = OrderConverter.toPaymentPO(payment);
        orderPaymentMapper.update(po, new LambdaUpdateWrapper<OrderPaymentPO>()
                .eq(OrderPaymentPO::getId, payment.getId())
                .eq(OrderPaymentPO::getTenantId, payment.getTenantId())
                .eq(payment.getOrderId() != null, OrderPaymentPO::getOrderId, payment.getOrderId()));
    }
}
