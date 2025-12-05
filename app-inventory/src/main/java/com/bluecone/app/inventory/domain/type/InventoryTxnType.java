package com.bluecone.app.inventory.domain.type;

import java.util.Arrays;

/**
 * 库存事务类型，覆盖锁定、释放、扣减、入出库、盘点等。
 */
public enum InventoryTxnType {

    LOCK("LOCK", "锁定库存"),
    UNLOCK("UNLOCK", "释放锁定库存"),
    DEDUCT("DEDUCT", "扣减库存"),
    INBOUND("INBOUND", "入库"),
    OUTBOUND("OUTBOUND", "出库"),
    ADJUST("ADJUST", "盘点调整");

    private final String code;
    private final String desc;

    InventoryTxnType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static InventoryTxnType fromCode(String code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
    }
}
