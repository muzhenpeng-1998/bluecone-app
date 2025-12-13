package com.bluecone.app.inventory.domain.error;

import com.bluecone.app.core.error.ErrorCode;

/**
 * 库存模块错误码定义。
 */
public enum InventoryErrorCode implements ErrorCode {

    INVENTORY_POLICY_NOT_FOUND("INV-404-001", "库存策略未配置");

    private final String code;
    private final String message;

    InventoryErrorCode(String code, String message) {
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

