package com.bluecone.app.order.domain.enums;

import java.util.Arrays;

/**
 * 业务业态类型。
 */
public enum BizType {

    COFFEE("COFFEE", "咖啡/饮品"),
    FOOD("FOOD", "正餐/简餐"),
    RETAIL("RETAIL", "零售/便利店/花店"),
    BEAUTY("BEAUTY", "美容/服务类"),
    VENUE("VENUE", "场地/包间/KTV"),
    OTHER("OTHER", "其他业态");

    private final String code;
    private final String desc;

    BizType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static BizType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
    }
}
