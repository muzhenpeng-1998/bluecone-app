package com.bluecone.app.inventory.domain.type;

import java.util.Arrays;

/**
 * 库存事务方向：增加或减少。
 */
public enum InventoryTxnDirection {

    IN("IN", "增加库存"),
    OUT("OUT", "减少库存");

    private final String code;
    private final String desc;

    InventoryTxnDirection(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static InventoryTxnDirection fromCode(String code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
    }
}
