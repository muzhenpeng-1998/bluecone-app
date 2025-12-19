package com.bluecone.app.order.domain.enums;

/**
 * 订单业务错误码枚举。
 * 
 * <h3>设计原则：</h3>
 * <ul>
 *   <li>错误码采用业务前缀 + 具体场景，便于快速定位问题</li>
 *   <li>中文错误消息，前端可直接展示给用户</li>
 *   <li>错误码与 HTTP 状态码解耦，统一返回 400/500 等标准状态码</li>
 * </ul>
 */
public enum OrderErrorCode {

    // ===== 订单状态相关 =====
    ORDER_NOT_FOUND("ORDER_NOT_FOUND", "订单不存在"),
    ORDER_NOT_BELONG_TO_STORE("ORDER_NOT_BELONG_TO_STORE", "订单不属于当前门店"),
    ORDER_STATUS_NOT_ALLOW_ACCEPT("ORDER_STATUS_NOT_ALLOW_ACCEPT", "订单状态不允许接单，请刷新后重试"),
    ORDER_STATUS_NOT_ALLOW_REJECT("ORDER_STATUS_NOT_ALLOW_REJECT", "订单状态不允许拒单，请刷新后重试"),
    ORDER_ALREADY_ACCEPTED("ORDER_ALREADY_ACCEPTED", "订单已被接单"),
    ORDER_ALREADY_REJECTED("ORDER_ALREADY_REJECTED", "订单已被拒单"),

    // ===== 并发控制相关 =====
    ORDER_VERSION_CONFLICT("ORDER_VERSION_CONFLICT", "订单版本冲突，请刷新后重试"),
    ORDER_CONCURRENT_MODIFICATION("ORDER_CONCURRENT_MODIFICATION", "订单正在被其他人操作，请稍后重试"),

    // ===== 参数校验相关 =====
    PARAM_INVALID("PARAM_INVALID", "参数校验失败"),
    REQUEST_ID_REQUIRED("REQUEST_ID_REQUIRED", "请求ID（requestId）不能为空"),
    REJECT_REASON_REQUIRED("REJECT_REASON_REQUIRED", "拒单原因不能为空"),

    // ===== 幂等相关 =====
    IDEMPOTENT_ACTION_FAILED("IDEMPOTENT_ACTION_FAILED", "该操作之前已失败，请使用新的请求ID重试"),
    ;

    private final String code;
    private final String message;

    OrderErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s", code, message);
    }
}
