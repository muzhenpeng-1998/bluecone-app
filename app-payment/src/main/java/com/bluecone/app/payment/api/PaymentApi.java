package com.bluecone.app.payment.api;

import com.bluecone.app.payment.api.command.CreatePaymentCommand;
import com.bluecone.app.payment.api.dto.CreatePaymentResult;
import com.bluecone.app.payment.api.dto.PaymentOrderView;

/**
 * 支付应用接口：
 * - 负责支付建单与查询的对外契约；
 * - 不直接暴露领域模型，使用 DTO/Command 进行交互。
 */
public interface PaymentApi {

    /**
     * 创建支付单（本地建单，不调用第三方渠道）。
     *
     * 后续步骤可在此基础上接入微信/支付宝预下单，并返回前端唤起支付参数。
     */
    CreatePaymentResult createPayment(CreatePaymentCommand command);

    /**
     * 根据支付单ID查询支付详情。
     */
    PaymentOrderView getPaymentById(Long paymentId);

    /**
     * 根据业务订单维度查询最近的一次支付单（预留）。
     */
    PaymentOrderView getLatestPaymentByBiz(String bizType, String bizOrderNo);
}
