package com.bluecone.app.payment.application;

import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.payment.api.WechatPayCallbackCommand;
import com.bluecone.app.payment.domain.model.PaymentOrder;
import com.bluecone.app.payment.domain.repository.PaymentOrderRepository;
import com.bluecone.app.payment.domain.service.PaymentDomainService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public WechatPayCallbackApplicationService(PaymentOrderRepository paymentOrderRepository,
                                               PaymentDomainService paymentDomainService) {
        this.paymentOrderRepository = paymentOrderRepository;
        this.paymentDomainService = paymentDomainService;
    }

    /**
     * 处理微信支付回调。
     *
     * @param command 微信回调命令对象
     */
    @Transactional
    public void handleWechatPayCallback(final WechatPayCallbackCommand command) {
        validateCommand(command);

        Long paymentId = parsePaymentId(command.getOutTradeNo());
        if (paymentId == null) {
            log.warn("[wechat-callback] 无法解析支付单号，outTradeNo={}", command.getOutTradeNo());
            return;
        }

        Optional<PaymentOrder> paymentOpt = paymentOrderRepository.findById(paymentId);
        if (paymentOpt.isEmpty()) {
            log.warn("[wechat-callback] 支付单不存在，可能是重复通知或数据已删除，outTradeNo={}, transactionId={}",
                    command.getOutTradeNo(), command.getTransactionId());
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

        log.info("[wechat-callback] 支付更新成功，paymentId={}, outTradeNo={}, transactionId={}, tradeState={}",
                paymentOrder.getId(), command.getOutTradeNo(), command.getTransactionId(), command.getTradeState());
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
