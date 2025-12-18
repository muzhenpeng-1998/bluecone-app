package com.bluecone.app.order.domain.enums;

/**
 * 订单状态机事件。
 * <p>描述导致状态变化的业务动作。</p>
 */
public enum OrderEvent {

    /**
     * 用户提交订单（从草稿/DRAFT 进入待支付/待处理阶段）。
     */
    SUBMIT,

    /**
     * 用户主动取消订单。
     */
    USER_CANCEL,

    /**
     * 商户取消订单（关店、缺货等原因）。
     */
    MERCHANT_CANCEL,

    /**
     * 商户接单。
     */
    MERCHANT_ACCEPT,

    /**
     * 支付成功（微信/支付宝等异步回调）。
     */
    PAY_SUCCESS,

    /**
     * 支付失败或关闭（例如微信关闭订单）。
     */
    PAY_FAILED,

    /**
     * 超时未支付自动取消。
     */
    AUTO_CANCEL_TIMEOUT,

    /**
     * 商户完成履约（无细分出餐/配送过程时，可直接视为完成）。
     */
    COMPLETE,

    /**
     * 全额退款完成。
     */
    FULL_REFUND,

    /**
     * 部分退款完成（订单状态可能保持不变，仅资金侧变化）。
     */
    PARTIAL_REFUND
}
