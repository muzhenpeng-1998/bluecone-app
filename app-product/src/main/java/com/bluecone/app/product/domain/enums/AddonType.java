package com.bluecone.app.product.domain.enums;

import java.util.Arrays;

/**
 * 小料组类型枚举，对应 bc_addon_group.type 字段。
 */
public enum AddonType {

    PRICED(1, "计价小料"),
    FREE(2, "不计价小料");

    private final int code;
    private final String description;

    AddonType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static AddonType fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(item -> item.code == code)
                .findFirst()
                .orElse(null);
    }
}
