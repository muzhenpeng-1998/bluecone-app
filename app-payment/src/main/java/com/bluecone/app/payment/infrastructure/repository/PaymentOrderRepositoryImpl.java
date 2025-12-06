package com.bluecone.app.payment.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.payment.domain.model.PaymentOrder;
import com.bluecone.app.payment.domain.repository.PaymentOrderRepository;
import com.bluecone.app.payment.infrastructure.converter.PaymentOrderConverter;
import com.bluecone.app.payment.infrastructure.persistence.PaymentOrderDO;
import com.bluecone.app.payment.infrastructure.persistence.PaymentOrderMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * 支付单仓储实现（MyBatis-Plus）。
 * <p>
 * - 通过 Converter 隔离领域对象与 DO；
 * - 提供简单的幂等查询与乐观锁更新能力。
 */
@Repository
public class PaymentOrderRepositoryImpl implements PaymentOrderRepository {

    private final PaymentOrderMapper paymentOrderMapper;

    public PaymentOrderRepositoryImpl(PaymentOrderMapper paymentOrderMapper) {
        this.paymentOrderMapper = paymentOrderMapper;
    }

    @Override
    public Optional<PaymentOrder> findById(Long id) {
        PaymentOrderDO doObj = paymentOrderMapper.selectById(id);
        return Optional.ofNullable(PaymentOrderConverter.toDomain(doObj));
    }

    @Override
    public Optional<PaymentOrder> findByPaymentNo(String paymentNo) {
        // 当前表未定义 payment_no 字段，预留实现
        return Optional.empty();
    }

    @Override
    public Optional<PaymentOrder> findByBizKeys(Long tenantId,
                                                String bizType,
                                                String bizOrderNo,
                                                String channelCode,
                                                String methodCode) {
        // 当前表包含 business_order_id + pay_channel，可用于部分幂等；bizType/method 未落库，留待后续扩展
        LambdaQueryWrapper<PaymentOrderDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentOrderDO::getTenantId, tenantId);
        if (channelCode != null) {
            wrapper.eq(PaymentOrderDO::getPayChannel, channelCode);
        }
        // bizOrderNo 未直接存储，若未来映射到 businessOrderId 可在此补充条件
        PaymentOrderDO doObj = paymentOrderMapper.selectOne(wrapper.last("limit 1"));
        return Optional.ofNullable(PaymentOrderConverter.toDomain(doObj));
    }

    @Override
    public void insert(PaymentOrder paymentOrder) {
        PaymentOrderDO doObj = PaymentOrderConverter.toDO(paymentOrder);
        paymentOrderMapper.insert(doObj);
        if (paymentOrder.getId() == null) {
            paymentOrder.setId(doObj.getId());
        }
        // MyBatis-Plus @Version 自动加一，若未启用插件则回填原值
        paymentOrder.setVersion(doObj.getVersion() == null ? paymentOrder.getVersion() : doObj.getVersion().intValue());
    }

    @Override
    public void update(PaymentOrder paymentOrder) {
        if (paymentOrder.getId() == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "支付单更新失败：ID 不能为空");
        }
        PaymentOrderDO doObj = PaymentOrderConverter.toDO(paymentOrder);
        Long currentVersion = doObj.getVersion() == null ? 0L : doObj.getVersion();

        LambdaUpdateWrapper<PaymentOrderDO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(PaymentOrderDO::getId, doObj.getId())
                .eq(PaymentOrderDO::getVersion, currentVersion)
                .set(PaymentOrderDO::getTenantId, doObj.getTenantId())
                .set(PaymentOrderDO::getStoreId, doObj.getStoreId())
                .set(PaymentOrderDO::getBusinessOrderId, doObj.getBusinessOrderId())
                .set(PaymentOrderDO::getUserId, doObj.getUserId())
                .set(PaymentOrderDO::getPayChannel, doObj.getPayChannel())
                .set(PaymentOrderDO::getPayScene, doObj.getPayScene())
                .set(PaymentOrderDO::getCurrency, doObj.getCurrency())
                .set(PaymentOrderDO::getTotalAmount, doObj.getTotalAmount())
                .set(PaymentOrderDO::getPayAmount, doObj.getPayAmount())
                .set(PaymentOrderDO::getDiscountAmount, doObj.getDiscountAmount())
                .set(PaymentOrderDO::getStatus, doObj.getStatus())
                .set(PaymentOrderDO::getExpireTime, doObj.getExpireTime())
                .set(PaymentOrderDO::getThirdTradeNo, doObj.getThirdTradeNo())
                .set(PaymentOrderDO::getUpdatedAt, doObj.getUpdatedAt())
                .set(PaymentOrderDO::getUpdatedBy, doObj.getUpdatedBy())
                .set(PaymentOrderDO::getVersion, currentVersion + 1);

        int rows = paymentOrderMapper.update(null, wrapper);
        if (rows == 0) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "支付单更新失败，版本冲突");
        }
        paymentOrder.setVersion(currentVersion.intValue() + 1);
    }

    @Override
    public long countCreatedBetween(LocalDateTime from, LocalDateTime to) {
        LambdaQueryWrapper<PaymentOrderDO> wrapper = new LambdaQueryWrapper<>();
        if (from != null) {
            wrapper.ge(PaymentOrderDO::getCreatedAt, from);
        }
        if (to != null) {
            wrapper.le(PaymentOrderDO::getCreatedAt, to);
        }
        return paymentOrderMapper.selectCount(wrapper);
    }

    @Override
    public long countSucceededBetween(LocalDateTime from, LocalDateTime to) {
        LambdaQueryWrapper<PaymentOrderDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentOrderDO::getStatus, com.bluecone.app.payment.domain.enums.PaymentStatus.SUCCESS.getCode());
        if (from != null) {
            wrapper.ge(PaymentOrderDO::getUpdatedAt, from);
        }
        if (to != null) {
            wrapper.le(PaymentOrderDO::getUpdatedAt, to);
        }
        return paymentOrderMapper.selectCount(wrapper);
    }

    @Override
    public long countStuckPayments(LocalDateTime before, List<com.bluecone.app.payment.domain.enums.PaymentStatus> statuses) {
        LambdaQueryWrapper<PaymentOrderDO> wrapper = new LambdaQueryWrapper<>();
        if (statuses != null && !statuses.isEmpty()) {
            wrapper.in(PaymentOrderDO::getStatus, statuses.stream().map(com.bluecone.app.payment.domain.enums.PaymentStatus::getCode).toList());
        }
        if (before != null) {
            wrapper.le(PaymentOrderDO::getUpdatedAt, before);
        }
        return paymentOrderMapper.selectCount(wrapper);
    }

    @Override
    public List<PaymentOrder> findByPayDate(java.time.LocalDate payDate) {
        LambdaQueryWrapper<PaymentOrderDO> wrapper = new LambdaQueryWrapper<>();
        if (payDate != null) {
            wrapper.ge(PaymentOrderDO::getUpdatedAt, payDate.atStartOfDay())
                    .lt(PaymentOrderDO::getUpdatedAt, payDate.plusDays(1).atStartOfDay());
        }
        List<PaymentOrderDO> list = paymentOrderMapper.selectList(wrapper);
        return list.stream().map(PaymentOrderConverter::toDomain).toList();
    }

    @Override
    public List<PaymentOrder> findSucceededByPayDate(java.time.LocalDate payDate) {
        LambdaQueryWrapper<PaymentOrderDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentOrderDO::getStatus, com.bluecone.app.payment.domain.enums.PaymentStatus.SUCCESS.getCode());
        if (payDate != null) {
            wrapper.ge(PaymentOrderDO::getUpdatedAt, payDate.atStartOfDay())
                    .lt(PaymentOrderDO::getUpdatedAt, payDate.plusDays(1).atStartOfDay());
        }
        List<PaymentOrderDO> list = paymentOrderMapper.selectList(wrapper);
        return list.stream().map(PaymentOrderConverter::toDomain).toList();
    }
}
