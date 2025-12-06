package com.bluecone.app.payment.domain.service.impl;

import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.payment.domain.enums.PaymentChannel;
import com.bluecone.app.payment.domain.enums.PaymentEvent;
import com.bluecone.app.payment.domain.enums.PaymentMethod;
import com.bluecone.app.payment.domain.enums.PaymentScene;
import com.bluecone.app.payment.domain.enums.PaymentStatus;
import com.bluecone.app.payment.domain.model.PaymentOrder;
import com.bluecone.app.payment.domain.service.PaymentDomainService;
import com.bluecone.app.payment.domain.service.PaymentStateMachine;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

/**
 * 支付领域服务默认实现：
 * - 聚合内的金额校验、状态机校验、幂等校验集中处理；
 * - 不做 DB/HTTP/SDK 调用，纯领域逻辑；
 * - 由应用层负责持久化与外部渠道交互。
 */
@Service
public class PaymentDomainServiceImpl implements PaymentDomainService {

    private final PaymentStateMachine paymentStateMachine;

    public PaymentDomainServiceImpl(PaymentStateMachine paymentStateMachine) {
        this.paymentStateMachine = paymentStateMachine;
    }

    @Override
    public PaymentOrder buildPaymentOrder(Long tenantId,
                                          Long storeId,
                                          String bizType,
                                          String bizOrderNo,
                                          PaymentChannel channel,
                                          PaymentMethod method,
                                          PaymentScene scene,
                                          BigDecimal totalAmount,
                                          BigDecimal discountAmount,
                                          String currency,
                                          String idempotentKey,
                                          LocalDateTime expireAt) {
        // 1. 基本参数校验
        if (tenantId == null || storeId == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "支付建单请求参数不合法：tenantId/storeId 不能为空");
        }
        if (isBlank(bizType) || isBlank(bizOrderNo)) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "支付建单请求参数不合法：bizType/bizOrderNo 不能为空");
        }
        if (channel == null || method == null || scene == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "支付建单请求参数不合法：channel/method/scene 不能为空");
        }

        // 2. 归一化金额并校验
        BigDecimal safeTotal = totalAmount == null ? BigDecimal.ZERO : totalAmount;
        BigDecimal safeDiscount = discountAmount == null ? BigDecimal.ZERO : discountAmount;
        if (safeTotal.compareTo(BigDecimal.ZERO) < 0 || safeDiscount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "支付建单请求参数不合法：金额不能为负数");
        }

        String safeCurrency = isBlank(currency) ? "CNY" : currency;

        // 3. 构建聚合根
        PaymentOrder paymentOrder = PaymentOrder.builder()
                .tenantId(tenantId)
                .storeId(storeId)
                .bizType(bizType)
                .bizOrderNo(bizOrderNo)
                .channel(channel)
                .method(method)
                .scene(scene)
                .currency(safeCurrency)
                .idempotentKey(idempotentKey)
                .expireAt(expireAt)
                .status(PaymentStatus.INIT)
                .version(0)
                .build();

        // 4. 金额初始化与状态初始化
        paymentOrder.initAmounts(safeTotal, safeDiscount);
        paymentOrder.markInit();

        return paymentOrder;
    }

    @Override
    public PaymentOrder applyPaySuccessNotification(PaymentOrder paymentOrder,
                                                    PaymentScene scene,
                                                    BigDecimal paidAmount,
                                                    String channelTradeNo,
                                                    LocalDateTime paidAt) {
        if (paymentOrder == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "支付回调处理失败：支付单不存在");
        }
        if (scene == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "支付回调处理失败：场景不能为空");
        }
        if (paidAmount == null || paidAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "支付回调处理失败：支付金额不合法");
        }
        if (isBlank(channelTradeNo)) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "支付回调处理失败：渠道交易号不能为空");
        }

        PaymentStatus current = paymentOrder.getStatus();
        PaymentEvent event = PaymentEvent.PAY_SUCCESS;
        String sceneCode = scene.getCode();
        if (!paymentStateMachine.canTransit(sceneCode, current, event)) {
            throw new BizException(CommonErrorCode.BAD_REQUEST,
                    String.format("当前支付状态不允许标记为成功：current=%s, event=%s, scene=%s",
                            current, event, sceneCode));
        }

        paymentOrder.markSuccess(paidAmount, channelTradeNo, paidAt);
        return paymentOrder;
    }

    @Override
    public PaymentOrder closeOrFailPayment(PaymentOrder paymentOrder,
                                           PaymentScene scene,
                                           String reasonCode,
                                           boolean timeout) {
        if (paymentOrder == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "支付关闭处理失败：支付单不存在");
        }
        if (scene == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "支付关闭处理失败：场景不能为空");
        }

        PaymentStatus current = paymentOrder.getStatus();
        String sceneCode = scene.getCode();

        if (timeout) {
            PaymentEvent event = PaymentEvent.PAY_TIMEOUT;
            if (!paymentStateMachine.canTransit(sceneCode, current, event)) {
                throw new BizException(CommonErrorCode.BAD_REQUEST,
                        String.format("当前支付状态不允许超时关闭：current=%s, event=%s, scene=%s",
                                current, event, sceneCode));
            }
            // 超时关闭走取消逻辑，保持与状态机约定一致
            paymentOrder.markCanceled();
            return paymentOrder;
        }

        // 渠道或业务失败场景
        if (current == PaymentStatus.SUCCESS || current == PaymentStatus.REFUNDED) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "已完成支付的订单不允许标记失败");
        }
        if (current == PaymentStatus.CANCELED || current == PaymentStatus.CLOSED) {
            // 幂等重入，直接返回
            return paymentOrder;
        }

        PaymentEvent event = PaymentEvent.PAY_FAILED;
        if (!paymentStateMachine.canTransit(sceneCode, current, event)) {
            throw new BizException(CommonErrorCode.BAD_REQUEST,
                    String.format("当前支付状态不允许标记失败：current=%s, event=%s, scene=%s",
                            current, event, sceneCode));
        }
        paymentOrder.markFailed(reasonCode);
        return paymentOrder;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
