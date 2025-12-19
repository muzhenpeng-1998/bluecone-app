package com.bluecone.app.billing.domain.error;

import com.bluecone.app.core.error.ErrorCode;

/**
 * 计费模块错误码定义。
 * <p>前缀 BL，表示 Billing 模块。</p>
 */
public enum BillingErrorCode implements ErrorCode {

    // ========= 订阅相关 =========
    SUBSCRIPTION_NOT_FOUND("BL-404-001", "订阅不存在"),
    SUBSCRIPTION_ALREADY_EXISTS("BL-409-001", "订阅已存在"),
    SUBSCRIPTION_STATUS_INVALID("BL-400-001", "订阅状态无效"),
    
    // ========= 套餐相关 =========
    PLAN_NOT_FOUND("BL-404-010", "套餐不存在"),
    PLAN_SKU_NOT_FOUND("BL-404-011", "套餐SKU不存在"),
    PLAN_FEATURE_RESTRICTED("BL-403-010", "当前套餐不支持此功能"),
    
    // ========= 宽限期相关 =========
    GRACE_PERIOD_WRITE_RESTRICTED("BL-403-020", "宽限期内限制写操作"),
    GRACE_PERIOD_FEATURE_RESTRICTED("BL-403-021", "宽限期内限制高级功能"),
    
    // ========= 订阅过期相关 =========
    SUBSCRIPTION_EXPIRED("BL-403-030", "订阅已过期"),
    SUBSCRIPTION_EXPIRED_WRITE_RESTRICTED("BL-403-031", "订阅已过期，限制写操作"),
    SUBSCRIPTION_EXPIRED_FEATURE_RESTRICTED("BL-403-032", "订阅已过期，限制高级功能"),
    SUBSCRIPTION_STATUS_RESTRICTED("BL-403-033", "订阅状态异常，限制操作"),
    
    // ========= 配额相关 =========
    FREE_PLAN_QUOTA_EXCEEDED("BL-403-040", "免费版配额已达上限"),
    PLAN_QUOTA_EXCEEDED("BL-403-041", "套餐配额已达上限"),
    
    // ========= 发票相关 =========
    INVOICE_NOT_FOUND("BL-404-050", "发票不存在"),
    INVOICE_ALREADY_PAID("BL-409-050", "发票已支付"),
    INVOICE_STATUS_INVALID("BL-400-050", "发票状态无效"),
    
    // ========= 提醒相关 =========
    REMINDER_TASK_NOT_FOUND("BL-404-060", "提醒任务不存在"),
    REMINDER_SEND_FAILED("BL-500-060", "提醒发送失败");

    private final String code;
    private final String message;

    BillingErrorCode(String code, String message) {
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
