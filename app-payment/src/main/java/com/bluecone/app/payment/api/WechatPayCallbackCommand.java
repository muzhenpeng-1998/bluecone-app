package com.bluecone.app.payment.api;

import java.time.Instant;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 微信支付回调命令对象，封装 HTTP 层解析出的通知字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WechatPayCallbackCommand {

    /**
     * 微信回调中的 appid。
     */
    private String appId;

    /**
     * 微信回调中的商户号 mchid。
     */
    private String mchId;

    /**
     * 商户订单号，对应我方支付单号（通常为 paymentId 或 paymentNo）。
     */
    private String outTradeNo;

    /**
     * 微信支付单号 transaction_id。
     */
    private String transactionId;

    /**
     * 总金额，单位：分。
     */
    private Long totalAmount;

    /**
     * 交易状态：SUCCESS / REFUND / NOTPAY / CLOSED 等。
     */
    private String tradeState;

    /**
     * 银行类型（可选）。
     */
    private String bankType;

    /**
     * 附加数据（attach），通常存放 tenantId/orderId 等 JSON 字符串。
     */
    private String attach;

    /**
     * 原始回调报文（JSON/XML），用于审计或排查。
     */
    private String rawBody;

    /**
     * 回调通知的唯一 ID（如有）。
     */
    private String notifyId;

    /**
     * 支付成功时间（如果回调中提供）。
     */
    private Instant successTime;

    /**
     * 预留字段，用于承载尚未建模的其它通知字段。
     */
    private Map<String, Object> extraFields;

    /**
     * TODO: 可根据回调 payload 构建命令对象，留待 Controller 集成时补充实现。
     */
    public static WechatPayCallbackCommand fromNotifyPayload(Map<String, Object> payload) {
        // TODO 填充解析逻辑
        return null;
    }
}
