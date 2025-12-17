package com.bluecone.app.store.domain.error;

import com.bluecone.app.core.error.ErrorCode;

/**
 * 店铺模块错误码定义。
 * <p>前缀 ST，表示 Store 模块。</p>
 */
public enum StoreErrorCode implements ErrorCode {

    // ========= 基础店铺 =========
    STORE_NOT_FOUND("ST-404-001", "门店不存在"),
    STORE_CONFIG_CONFLICT("ST-409-001", "门店配置版本冲突，请刷新后重试"),
    STORE_STATUS_NOT_OPEN("ST-400-001", "门店未处于营业状态"),
    STORE_NOT_ACCEPTING_ORDERS("ST-400-002", "门店暂不接单"),
    STORE_OUT_OF_BUSINESS_HOURS("ST-400-003", "当前不在营业时间内"),
    STORE_NO_OPENING_CONFIG("ST-400-004", "门店未配置营业时间"),
    STORE_CAPABILITY_DISABLED("ST-400-005", "当前服务类型暂不支持"),
    STORE_DISABLED("ST-410-001", "门店已停用"),
    STORE_CLOSED_FOR_ORDERS("ST-409-002", "门店当前不可接单"),

    // ========= 渠道相关 =========
    CHANNEL_NOT_FOUND("ST-404-010", "门店渠道不存在"),
    CHANNEL_ALREADY_BOUND("ST-409-010", "该渠道已绑定，请勿重复绑定"),
    STORE_CHANNEL_NOT_BOUND("ST-400-010", "门店未绑定该渠道或渠道未启用"),

    // ========= 资源相关 =========
    RESOURCE_NOT_FOUND("ST-404-020", "门店资源不存在"),

    // ========= 设备相关 =========
    DEVICE_NOT_FOUND("ST-404-030", "门店设备不存在"),

    // ========= 打印规则相关 =========
    PRINT_RULE_NOT_FOUND("ST-404-040", "打印规则不存在"),

    // ========= 员工相关 =========
    STAFF_ALREADY_EXISTS("ST-409-050", "该员工已存在于门店"),
    STAFF_NOT_FOUND("ST-404-050", "门店员工不存在");

    private final String code;
    private final String message;

    StoreErrorCode(String code, String message) {
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
