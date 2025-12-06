package com.bluecone.app.payment.application;

import com.bluecone.app.core.event.DomainEventPublisher;
import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.payment.api.WechatPayCallbackCommand;
import com.bluecone.app.payment.domain.enums.PaymentStatus;
import com.bluecone.app.payment.domain.model.PaymentOrder;
import com.bluecone.app.payment.domain.repository.PaymentOrderRepository;
import com.bluecone.app.payment.domain.service.PaymentDomainService;
import com.bluecone.app.payment.event.PaymentSucceededEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 微信支付异步通知的应用服务。
 * <p>
 * - 负责幂等处理回调，将支付单标记为成功；<br>
 * - 不关心 HTTP 层与返回格式，Controller 会在上层模块添加；<br>
 * - 暂不发布领域事件，后续可在此基础上扩展 Outbox。
 */
@Service
public class WechatPayCallbackApplicationService {

    private static final Logger log = LoggerFactory.getLogger(WechatPayCallbackApplicationService.class);

    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentDomainService paymentDomainService;
    private final DomainEventPublisher domainEventPublisher;
    private final PaymentMetricsRecorder paymentMetricsRecorder;

    public WechatPayCallbackApplicationService(PaymentOrderRepository paymentOrderRepository,
                                               PaymentDomainService paymentDomainService,
                                               DomainEventPublisher domainEventPublisher,
                                               PaymentMetricsRecorder paymentMetricsRecorder) {
        this.paymentOrderRepository = paymentOrderRepository;
        this.paymentDomainService = paymentDomainService;
        this.domainEventPublisher = domainEventPublisher;
        this.paymentMetricsRecorder = paymentMetricsRecorder;
    }

    /**
     * 处理微信支付回调。
     *
     * @param command 微信回调命令对象
     */
    @Transactional
    public void handleWechatPayCallback(final WechatPayCallbackCommand command) {
        long startNanos = System.nanoTime();
        String resultTag = "SUCCESS";
        String methodCode = "UNKNOWN";
        log.info("[wechat-callback] traceId={} outTradeNo={} transactionId={} tradeState={}",
                MDC.get("traceId"), command.getOutTradeNo(), command.getTransactionId(), command.getTradeState());
        try {
            validateCommand(command);

            Long paymentId = parsePaymentId(command.getOutTradeNo());
            if (paymentId == null) {
                log.warn("[wechat-callback] 无法解析支付单号，outTradeNo={}", command.getOutTradeNo());
                resultTag = "FAIL";
                return;
            }

            Optional<PaymentOrder> paymentOpt = paymentOrderRepository.findById(paymentId);
            if (paymentOpt.isEmpty()) {
                log.warn("[wechat-callback] 支付单不存在，可能是重复通知或数据已删除，outTradeNo={}, transactionId={}",
                        command.getOutTradeNo(), command.getTransactionId());
                resultTag = "FAIL";
                return;
            }

            PaymentOrder paymentOrder = paymentOpt.get();

            // 金额校验（分）
            if (command.getTotalAmount() != null && paymentOrder.getPayableAmount() != null) {
                long expectedFen = toFen(paymentOrder.getPayableAmount());
                if (!expectedFenEquals(expectedFen, command.getTotalAmount())) {
                    log.error("[wechat-callback] 金额不一致，outTradeNo={}, transactionId={}, expectFen={}, notifyFen={}",
                            command.getOutTradeNo(), command.getTransactionId(), expectedFen, command.getTotalAmount());
                    throw new BizException(CommonErrorCode.BAD_REQUEST, "支付金额不一致");
                }
            }

            if (!"SUCCESS".equalsIgnoreCase(command.getTradeState())) {
                log.info("[wechat-callback] 非 SUCCESS 状态暂不处理，tradeState={}, outTradeNo={}, transactionId={}",
                        command.getTradeState(), command.getOutTradeNo(), command.getTransactionId());
                resultTag = "FAIL";
                return;
            }

            methodCode = paymentOrder.getMethod() == null ? "UNKNOWN" : paymentOrder.getMethod().getCode();
            PaymentStatus currentStatus = paymentOrder.getStatus();
            if (currentStatus == PaymentStatus.SUCCESS) {
                log.warn("[wechat-callback] 支付单已成功，忽略重复通知，paymentId={}, outTradeNo={}, transactionId={}",
                        paymentOrder.getId(), command.getOutTradeNo(), command.getTransactionId());
                resultTag = "IDEMPOTENT";
                return;
            }
            if (currentStatus == PaymentStatus.CLOSED || currentStatus == PaymentStatus.REFUNDED) {
                log.warn("[wechat-callback] 支付单已终态，忽略通知，paymentId={}, status={}, outTradeNo={}, transactionId={}",
                        paymentOrder.getId(), currentStatus, command.getOutTradeNo(), command.getTransactionId());
                resultTag = "IDEMPOTENT";
                return;
            }

            BigDecimal paidAmount = command.getTotalAmount() == null
                    ? paymentOrder.getPayableAmount()
                    : toYuan(command.getTotalAmount());
            LocalDateTime paidAt = command.getSuccessTime() == null
                    ? LocalDateTime.now()
                    : LocalDateTime.ofInstant(command.getSuccessTime(), ZoneId.systemDefault());

            paymentOrder = paymentDomainService.applyPaySuccessNotification(
                    paymentOrder,
                    paymentOrder.getScene(),
                    paidAmount,
                    command.getTransactionId(),
                    paidAt
            );

            paymentOrderRepository.update(paymentOrder);

            PaymentSucceededEvent event = new PaymentSucceededEvent(
                    paymentOrder.getTenantId(),
                    paymentOrder.getStoreId(),
                    paymentOrder.getUserId(),
                    paymentOrder.getBizOrderId(),
                    paymentOrder.getId(),
                    paymentOrder.getChannel() == null ? null : paymentOrder.getChannel().getCode(),
                    paymentOrder.getPaidAmount(),
                    paymentOrder.getCurrency(),
                    paymentOrder.getPaidAt(),
                    paymentOrder.getChannelTradeNo(),
                    MDC.get("traceId")
            );
            domainEventPublisher.publish(event);

            log.info("[wechat-callback] 支付更新成功并发布事件，paymentId={}, orderId={}, outTradeNo={}, transactionId={}, tradeState={}, traceId={}",
                    paymentOrder.getId(),
                    paymentOrder.getBizOrderId(),
                    command.getOutTradeNo(),
                    command.getTransactionId(),
                    command.getTradeState(),
                    event.getTraceId());
        } catch (BizException ex) {
            resultTag = "FAIL";
            log.warn("[wechat-callback] biz error traceId={} paymentId={} outTradeNo={} transactionId={} msg={}",
                    MDC.get("traceId"), parsePaymentId(command.getOutTradeNo()), command.getOutTradeNo(), command.getTransactionId(), ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            resultTag = "ERROR";
            log.error("[wechat-callback] system error traceId={} outTradeNo={} transactionId={}",
                    MDC.get("traceId"), command.getOutTradeNo(), command.getTransactionId(), ex);
            throw ex;
        } finally {
            paymentMetricsRecorder.recordCallback("WECHAT", methodCode, resultTag, startNanos);
        }
    }

    private void validateCommand(WechatPayCallbackCommand command) {
        if (isBlank(command.getOutTradeNo()) || isBlank(command.getTransactionId()) || isBlank(command.getTradeState())) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "微信回调缺少必要字段");
        }
    }

    private boolean expectedFenEquals(long expectedFen, Long notifyFen) {
        return notifyFen != null && expectedFen == notifyFen;
    }

    private long toFen(BigDecimal amountYuan) {
        return amountYuan.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    private BigDecimal toYuan(Long fen) {
        return BigDecimal.valueOf(fen).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private Long parsePaymentId(String outTradeNo) {
        try {
            return Long.valueOf(outTradeNo);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
