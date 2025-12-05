package com.bluecone.app.inventory.domain.type;

import java.util.Arrays;

/**
 * 库存扣减模式：下单、支付、确认等阶段扣减。
 */
public enum InventoryDeductMode {

    ON_ORDER("ON_ORDER", "下单即扣"),
    ON_PAID("ON_PAID", "支付成功扣减"),
    ON_CONFIRM("ON_CONFIRM", "确认后扣减");

    private final String code;
    private final String desc;

    InventoryDeductMode(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static InventoryDeductMode fromCode(String code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
    }
}
