package com.bluecone.app.payment.domain.reconcile;

/**
 * 对账差异类型。
 */
public enum ReconcileDiffType {
    CHANNEL_ONLY,
    LOCAL_ONLY,
    AMOUNT_MISMATCH,
    STATUS_MISMATCH,
    FEE_MISMATCH
}
