package com.bluecone.app.payment.domain.repository;

import com.bluecone.app.payment.domain.model.PaymentOrder;
import java.util.Optional;

/**
 * 支付单聚合仓储接口。
 * <p>
 * - 隔离领域层与具体持久化技术；
 * - 只暴露领域对象，不暴露 DO/Mapper；
 * - 幂等、防重等能力在实现层结合唯一索引完成。
 */
public interface PaymentOrderRepository {

    /**
     * 根据主键ID查询支付单。
     */
    Optional<PaymentOrder> findById(Long id);

    /**
     * 根据支付单号查询支付单（若当前表未有 payment_no，可返回 Optional.empty() 并在后续扩展）。
     */
    Optional<PaymentOrder> findByPaymentNo(String paymentNo);

    /**
     * 根据业务维度查询支付单（用于幂等，防止重复建单）。
     *
     * @param tenantId   租户ID
     * @param bizType    业务类型
     * @param bizOrderNo 业务订单号
     * @param channelCode 渠道编码
     * @param methodCode  支付方式编码
     */
    Optional<PaymentOrder> findByBizKeys(Long tenantId,
                                         String bizType,
                                         String bizOrderNo,
                                         String channelCode,
                                         String methodCode);

    /**
     * 新增支付单（仅用于新建）。
     * <p>
     * 若支付单 ID 为空，由持久化层生成后回填。
     */
    void insert(PaymentOrder paymentOrder);

    /**
     * 更新支付单（状态机流转后的状态更新）。
     * <p>
     * 需通过乐观锁保证并发安全，更新成功后回填最新 version。
     *
     * @throws com.bluecone.app.core.exception.BizException 当乐观锁冲突时抛出
     */
    void update(PaymentOrder paymentOrder);
}
