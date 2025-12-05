package com.bluecone.app.inventory.domain.type;

import java.util.Arrays;

/**
 * 库存对象类型，覆盖商品 SKU、物料、服务档期、券模板等。
 */
public enum InventoryItemType {

    PRODUCT_SKU("PRODUCT_SKU", "标准商品 SKU"),
    MATERIAL("MATERIAL", "物料"),
    SERVICE_SLOT("SERVICE_SLOT", "服务档期"),
    VOUCHER_TEMPLATE("VOUCHER_TEMPLATE", "券模板");

    private final String code;
    private final String desc;

    InventoryItemType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static InventoryItemType fromCode(String code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
    }
}
