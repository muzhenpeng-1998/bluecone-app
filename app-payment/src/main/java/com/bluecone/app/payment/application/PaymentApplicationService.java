package com.bluecone.app.payment.application;

import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.payment.api.PaymentApi;
import com.bluecone.app.payment.api.command.CreatePaymentCommand;
import com.bluecone.app.payment.api.dto.CreatePaymentResult;
import com.bluecone.app.payment.api.dto.PaymentOrderView;
import com.bluecone.app.payment.domain.channel.PaymentChannelConfig;
import com.bluecone.app.payment.domain.channel.PaymentChannelConfigRepository;
import com.bluecone.app.payment.domain.channel.PaymentChannelType;
import com.bluecone.app.payment.domain.enums.PaymentChannel;
import com.bluecone.app.payment.domain.enums.PaymentMethod;
import com.bluecone.app.payment.domain.enums.PaymentScene;
import com.bluecone.app.payment.domain.enums.PaymentStatus;
import com.bluecone.app.payment.domain.gateway.WeChatJsapiPrepayRequest;
import com.bluecone.app.payment.domain.gateway.WeChatJsapiPrepayResponse;
import com.bluecone.app.payment.domain.gateway.WeChatPaymentGateway;
import com.bluecone.app.payment.domain.model.PaymentOrder;
import com.bluecone.app.payment.domain.repository.PaymentOrderRepository;
import com.bluecone.app.payment.domain.service.PaymentDomainService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
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
    private final PaymentChannelConfigRepository paymentChannelConfigRepository;
    private final WeChatPaymentGateway weChatPaymentGateway;
    private final ObjectMapper objectMapper;

    public PaymentApplicationService(PaymentDomainService paymentDomainService,
                                     PaymentOrderRepository paymentOrderRepository,
                                     PaymentChannelConfigRepository paymentChannelConfigRepository,
                                     WeChatPaymentGateway weChatPaymentGateway,
                                     ObjectMapper objectMapper) {
        this.paymentDomainService = paymentDomainService;
        this.paymentOrderRepository = paymentOrderRepository;
        this.paymentChannelConfigRepository = paymentChannelConfigRepository;
        this.weChatPaymentGateway = weChatPaymentGateway;
        this.objectMapper = objectMapper;
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

        // 微信 JSAPI 必填 openId
        if (channel == PaymentChannel.WECHAT && method == PaymentMethod.WECHAT_JSAPI) {
            if (isBlank(command.getPayerOpenId())) {
                throw new BizException(CommonErrorCode.BAD_REQUEST, "微信 JSAPI 支付需要提供 payerOpenId");
            }
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
            PaymentStatus status = order.getStatus();
            if (status != PaymentStatus.FAILED && status != PaymentStatus.CANCELED && status != PaymentStatus.CLOSED) {
                return toCreateResult(order, order.getChannelContext());
            }
        }

        String currency = isBlank(command.getCurrency()) ? "CNY" : command.getCurrency();
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
                currency,
                command.getIdempotentKey(),
                expireAt
        );
        paymentOrder.setUserId(command.getUserId());

        paymentOrderRepository.insert(paymentOrder);

        Map<String, Object> channelContext = null;
        PaymentChannelType channelType = PaymentChannelType.fromChannelAndMethod(channel, method);

        if (channelType == PaymentChannelType.WECHAT_JSAPI) {
            PaymentChannelConfig config = paymentChannelConfigRepository.findByTenantStoreAndChannel(
                            command.getTenantId(),
                            command.getStoreId(),
                            channelType
                    )
                    .orElseThrow(() -> new BizException(CommonErrorCode.BAD_REQUEST, "微信 JSAPI 渠道未配置"));

            BigDecimal payable = paymentOrder.getPayableAmount();
            if (payable == null) {
                throw new BizException(CommonErrorCode.SYSTEM_ERROR, "支付单缺少应付金额");
            }
            long fen = payable.multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValueExact();

            WeChatJsapiPrepayRequest req = new WeChatJsapiPrepayRequest();
            req.setPaymentOrderId(paymentOrder.getId());
            req.setTenantId(paymentOrder.getTenantId());
            req.setStoreId(paymentOrder.getStoreId());
            req.setUserId(paymentOrder.getUserId());
            req.setAmountTotal(fen);
            req.setCurrency(currency);
            req.setDescription(isBlank(command.getDescription()) ? buildDefaultDescription(command) : command.getDescription());
            req.setOutTradeNo(String.valueOf(paymentOrder.getId()));
            req.setPayerOpenId(command.getPayerOpenId());
            req.setAttach(buildAttachJson(paymentOrder));

            WeChatJsapiPrepayResponse resp = weChatPaymentGateway.jsapiPrepay(req, config);
            channelContext = new HashMap<>();
            channelContext.put("appId", resp.getAppId());
            channelContext.put("timeStamp", resp.getTimeStamp());
            channelContext.put("nonceStr", resp.getNonceStr());
            channelContext.put("package", resp.getPackageValue());
            channelContext.put("signType", resp.getSignType());
            channelContext.put("paySign", resp.getPaySign());
        }

        return toCreateResult(paymentOrder, channelContext);
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

    private CreatePaymentResult toCreateResult(PaymentOrder order, Map<String, Object> channelContext) {
        CreatePaymentResult result = new CreatePaymentResult();
        result.setPaymentId(order.getId());
        result.setPaymentNo(order.getPaymentNo() == null ? String.valueOf(order.getId()) : order.getPaymentNo());
        result.setBizType(order.getBizType());
        result.setBizOrderNo(order.getBizOrderNo());
        result.setChannelCode(order.getChannel() == null ? null : order.getChannel().getCode());
        result.setMethodCode(order.getMethod() == null ? null : order.getMethod().getCode());
        result.setSceneCode(order.getScene() == null ? null : order.getScene().getCode());
        result.setTotalAmount(order.getTotalAmount());
        result.setDiscountAmount(order.getDiscountAmount());
        result.setPayableAmount(order.getPayableAmount());
        result.setStatus(order.getStatus() == null ? null : order.getStatus().getCode());
        result.setChannelContext(channelContext);
        return result;
    }

    private PaymentOrderView toView(PaymentOrder order) {
        PaymentOrderView view = new PaymentOrderView();
        view.setPaymentId(order.getId());
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

    private String buildDefaultDescription(CreatePaymentCommand command) {
        return "BlueCone订单-" + command.getBizOrderNo();
    }

    private String buildAttachJson(PaymentOrder order) {
        Map<String, Object> attach = new HashMap<>();
        attach.put("tenantId", order.getTenantId());
        attach.put("storeId", order.getStoreId());
        attach.put("bizType", order.getBizType());
        attach.put("bizOrderNo", order.getBizOrderNo());
        try {
            return objectMapper.writeValueAsString(attach);
        } catch (JsonProcessingException e) {
            throw new BizException(CommonErrorCode.SYSTEM_ERROR, "支付附加信息序列化失败", e);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
