package com.bluecone.app.application.payment;

import com.bluecone.app.payment.api.PaymentApi;
import com.bluecone.app.payment.api.command.CreatePaymentCommand;
import com.bluecone.app.payment.api.dto.CreatePaymentResult;
import com.bluecone.app.payment.api.dto.PaymentOrderView;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 支付相关的 REST 接口：
 * - 创建支付单（含微信 JSAPI 预下单等）；
 * - 查询支付单。
 */
@Tag(name = "🔌 第三方集成 > 支付相关 > 支付接口", description = "支付创建和查询接口")
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentApi paymentApi;

    public PaymentController(PaymentApi paymentApi) {
        this.paymentApi = paymentApi;
    }

    /**
     * 创建支付单（包含微信 JSAPI 等渠道的预下单）。
     * <p>
     * 前端需在请求体中传入 CreatePaymentCommand：
     * - 微信 JSAPI 场景需携带 payerOpenId，channelCode=WECHAT，methodCode=WECHAT_JSAPI；
     * - 返回结果中的 channelContext 用于前端唤起支付。
     */
    @PostMapping
    public CreatePaymentResult createPayment(@Valid @RequestBody CreatePaymentCommand command) {
        return paymentApi.createPayment(command);
    }

    /**
     * 根据支付单 ID 查询详情。
     */
    @GetMapping("/{id}")
    public PaymentOrderView getPayment(@PathVariable("id") Long id) {
        return paymentApi.getPaymentById(id);
    }

    /**
     * 根据业务订单维度查询最近的一笔支付单。
     * 示例：/api/payments/by-biz?bizType=ORDER&bizOrderNo=202501010001
     */
    @GetMapping("/by-biz")
    public PaymentOrderView getLatestByBiz(@RequestParam("bizType") String bizType,
                                           @RequestParam("bizOrderNo") String bizOrderNo) {
        return paymentApi.getLatestPaymentByBiz(bizType, bizOrderNo);
    }
}
