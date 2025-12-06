package com.bluecone.app.payment.domain.reconcile;

/**
 * 渠道交易类型（用于账单与对账）。
 */
public enum ChannelTradeType {
    PAY,
    REFUND,
    FEE,
    OTHER
}
