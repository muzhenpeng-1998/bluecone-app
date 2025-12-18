package com.bluecone.app.order.domain.error;

import com.bluecone.app.core.error.ErrorCode;

/**
 * 订单域专属错误码。
 * <p>前缀 OR 代表 Order 模块。</p>
 */
public enum OrderErrorCode implements ErrorCode {

    ORDER_NOT_FOUND("OR-404-001", "订单不存在"),
    ORDER_STATE_INVALID("OR-400-001", "订单状态不允许当前操作"),
    ORDER_INVALID("OR-400-002", "订单数据非法"),
    /**
     * 门店不可接单，具体原因见 detail 字段。
     * <p>此错误码用于统一包装门店前置校验失败，实际原因码来自 StoreErrorCode。</p>
     */
    STORE_NOT_ACCEPTABLE("OR-400-003", "门店当前不可接单");

    private final String code;
    private final String message;

    OrderErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
