package com.bluecone.app.payment.domain.service;

import com.bluecone.app.payment.domain.enums.PaymentChannel;
import com.bluecone.app.payment.domain.enums.PaymentMethod;
import com.bluecone.app.payment.domain.enums.PaymentScene;
import com.bluecone.app.payment.domain.model.PaymentOrder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付领域服务：
 * - 聚合内的金额校验、状态机校验、幂等校验的集中入口；
 * - 纯领域逻辑，不做 DB/HTTP/SDK 调用；
 * - 由 Application/Infra 层负责持久化与对接渠道。
 */
public interface PaymentDomainService {

    /**
     * 构建一笔新的支付单聚合（尚未持久化）。
     *
     * 说明：
     * - 仅负责领域内数据构造与校验，不做 DB/HTTP 调用；
     * - 金额规则：totalAmount >= 0、discountAmount >= 0、payableAmount = total - discount >= 0；
     * - 初始状态设为 INIT，是否进入 PENDING 由上层流程控制。
     */
    PaymentOrder buildPaymentOrder(Long tenantId,
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
                                   LocalDateTime expireAt);

    /**
     * 在领域层应用“支付成功通知”。
     *
     * 使用规则：
     * - 通过 PaymentStateMachine 校验当前状态是否允许 PAY_SUCCESS 事件；
     * - 调用 PaymentOrder.markSuccess(...) 完成金额、渠道流水号、支付时间更新；
     * - 若为重复回调且关键字段一致，视为幂等重放，直接返回；
     * - 状态非法则抛出 BizException。
     */
    PaymentOrder applyPaySuccessNotification(PaymentOrder paymentOrder,
                                             PaymentScene scene,
                                             BigDecimal paidAmount,
                                             String channelTradeNo,
                                             LocalDateTime paidAt);

    /**
     * 关闭或标记支付失败。
     *
     * - timeout=true 视为超时关闭（PAY_TIMEOUT），通常进入 CANCELED；
     * - timeout=false 视为渠道或业务失败（PAY_FAILED），通常进入 FAILED 或 CANCELED；
     * - 具体流转由 PaymentStateMachine 决定。
     */
    PaymentOrder closeOrFailPayment(PaymentOrder paymentOrder,
                                    PaymentScene scene,
                                    String reasonCode,
                                    boolean timeout);
}
