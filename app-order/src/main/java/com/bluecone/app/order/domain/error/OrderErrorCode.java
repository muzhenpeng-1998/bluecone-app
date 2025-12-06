package com.bluecone.app.order.domain.error;

import com.bluecone.app.core.error.ErrorCode;

/**
 * 订单域专属错误码。
 * <p>前缀 OR 代表 Order 模块。</p>
 */
public enum OrderErrorCode implements ErrorCode {

    ORDER_NOT_FOUND("OR-404-001", "订单不存在"),
    ORDER_STATE_INVALID("OR-400-001", "订单状态不允许当前操作"),
    ORDER_INVALID("OR-400-002", "订单数据非法");

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
