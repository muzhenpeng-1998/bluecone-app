package com.bluecone.app.inventory.domain.type;

import java.util.Arrays;

/**
 * 库存锁定状态，对应 bc_inv_stock_lock.status。
 */
public enum InventoryLockStatus {

    LOCKED(0, "锁定中"),
    CONFIRMED(1, "已确认扣减"),
    RELEASED(2, "已释放"),
    EXPIRED(3, "已过期释放");

    private final int code;
    private final String desc;

    InventoryLockStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static InventoryLockStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(item -> item.code == code)
                .findFirst()
                .orElse(null);
    }
}
