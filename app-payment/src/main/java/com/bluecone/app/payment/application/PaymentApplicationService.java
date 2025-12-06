package com.bluecone.app.payment.application;

import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.payment.api.PaymentApi;
import com.bluecone.app.payment.api.command.CreatePaymentCommand;
import com.bluecone.app.payment.api.dto.CreatePaymentResult;
import com.bluecone.app.payment.api.dto.PaymentOrderView;
import com.bluecone.app.payment.domain.enums.PaymentChannel;
import com.bluecone.app.payment.domain.enums.PaymentMethod;
import com.bluecone.app.payment.domain.enums.PaymentScene;
import com.bluecone.app.payment.domain.model.PaymentOrder;
import com.bluecone.app.payment.domain.repository.PaymentOrderRepository;
import com.bluecone.app.payment.domain.service.PaymentDomainService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * 支付应用服务：实现 PaymentApi，承上层调用与领域服务之间的编排。
 * <p>
 * - 负责参数校验（依赖 @Validated + @Valid）；
 * - 负责幂等查询、构建聚合并落库；
 * - 不对接第三方支付渠道，后续步骤再扩展。
 */
@Service
@Validated
public class PaymentApplicationService implements PaymentApi {

    private static final int DEFAULT_EXPIRE_MINUTES = 15;

    private final PaymentDomainService paymentDomainService;
    private final PaymentOrderRepository paymentOrderRepository;

    public PaymentApplicationService(PaymentDomainService paymentDomainService,
                                     PaymentOrderRepository paymentOrderRepository) {
        this.paymentDomainService = paymentDomainService;
        this.paymentOrderRepository = paymentOrderRepository;
    }

    @Override
    public CreatePaymentResult createPayment(@Valid CreatePaymentCommand command) {
        PaymentChannel channel = PaymentChannel.fromCode(command.getChannelCode());
        PaymentMethod method = PaymentMethod.fromCode(command.getMethodCode());
        PaymentScene scene = PaymentScene.fromCode(command.getSceneCode());
        if (channel == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "不支持的支付渠道");
        }
        if (method == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "不支持的支付方式");
        }
        if (scene == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "不支持的支付场景");
        }

        int expireMinutes = command.getExpireMinutes() == null || command.getExpireMinutes() <= 0
                ? DEFAULT_EXPIRE_MINUTES
                : command.getExpireMinutes();
        LocalDateTime expireAt = LocalDateTime.now().plusMinutes(expireMinutes);

        // 幂等：按租户 + 业务订单 + 渠道 + 方式查已有支付单（当前 schema 支持有限，未来完善）
        Optional<PaymentOrder> existing = paymentOrderRepository.findByBizKeys(
                command.getTenantId(),
                command.getBizType(),
                command.getBizOrderNo(),
                channel.getCode(),
                method.getCode()
        );
        if (existing.isPresent()) {
            PaymentOrder order = existing.get();
            return toCreateResult(order);
        }

        PaymentOrder paymentOrder = paymentDomainService.buildPaymentOrder(
                command.getTenantId(),
                command.getStoreId(),
                command.getBizType(),
                command.getBizOrderNo(),
                channel,
                method,
                scene,
                command.getTotalAmount(),
                command.getDiscountAmount(),
                command.getCurrency(),
                command.getIdempotentKey(),
                expireAt
        );
        paymentOrder.setUserId(command.getUserId());

        paymentOrderRepository.insert(paymentOrder);
        return toCreateResult(paymentOrder);
    }

    @Override
    public PaymentOrderView getPaymentById(Long paymentId) {
        PaymentOrder order = paymentOrderRepository.findById(paymentId)
                .orElseThrow(() -> new BizException(CommonErrorCode.BAD_REQUEST, "支付单不存在"));
        return toView(order);
    }

    @Override
    public PaymentOrderView getLatestPaymentByBiz(String bizType, String bizOrderNo) {
        // 当前仓储未提供直接查询最近支付单能力，先返回 null/预留实现
        return null;
    }

    private CreatePaymentResult toCreateResult(PaymentOrder order) {
        CreatePaymentResult result = new CreatePaymentResult();
        result.setPaymentId(order.getId());
        result.setPaymentNo(order.getPaymentNo());
        result.setBizType(order.getBizType());
        result.setBizOrderNo(order.getBizOrderNo());
        result.setChannelCode(order.getChannel() == null ? null : order.getChannel().getCode());
        result.setMethodCode(order.getMethod() == null ? null : order.getMethod().getCode());
        result.setSceneCode(order.getScene() == null ? null : order.getScene().getCode());
        result.setTotalAmount(order.getTotalAmount());
        result.setDiscountAmount(order.getDiscountAmount());
        result.setPayableAmount(order.getPayableAmount());
        result.setStatus(order.getStatus() == null ? null : order.getStatus().getCode());
        result.setChannelContext(order.getChannelContext() == null ? Map.of() : order.getChannelContext());
        return result;
    }

    private PaymentOrderView toView(PaymentOrder order) {
        PaymentOrderView view = new PaymentOrderView();
        view.setId(order.getId());
        view.setPaymentNo(order.getPaymentNo());
        view.setTenantId(order.getTenantId());
        view.setStoreId(order.getStoreId());
        view.setBizType(order.getBizType());
        view.setBizOrderNo(order.getBizOrderNo());
        view.setChannelCode(order.getChannel() == null ? null : order.getChannel().getCode());
        view.setMethodCode(order.getMethod() == null ? null : order.getMethod().getCode());
        view.setSceneCode(order.getScene() == null ? null : order.getScene().getCode());
        view.setTotalAmount(order.getTotalAmount());
        view.setDiscountAmount(order.getDiscountAmount());
        view.setPayableAmount(order.getPayableAmount());
        view.setPaidAmount(order.getPaidAmount());
        view.setCurrency(order.getCurrency());
        view.setStatus(order.getStatus() == null ? null : order.getStatus().getCode());
        view.setExpireAt(order.getExpireAt());
        view.setPaidAt(order.getPaidAt());
        view.setChannelTradeNo(order.getChannelTradeNo());
        view.setChannelContext(order.getChannelContext() == null ? Map.of() : order.getChannelContext());
        view.setCreatedAt(order.getCreatedAt());
        view.setUpdatedAt(order.getUpdatedAt());
        return view;
    }
}
