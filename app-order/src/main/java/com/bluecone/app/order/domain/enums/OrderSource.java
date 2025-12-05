package com.bluecone.app.order.domain.enums;

import java.util.Arrays;

/**
 * 下单场景来源。
 */
public enum OrderSource {

    DINE_IN("DINE_IN", "堂食"),
    TAKEAWAY("TAKEAWAY", "到店自取"),
    DELIVERY("DELIVERY", "外卖配送"),
    BOOKING("BOOKING", "预约/预订"),
    PICKUP("PICKUP", "门店取货/零售自提"),
    RETAIL("RETAIL", "即买即走零售");

    private final String code;
    private final String desc;

    OrderSource(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static OrderSource fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
    }
}
