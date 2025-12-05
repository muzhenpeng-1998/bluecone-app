package com.bluecone.app.product.domain.enums;

import java.util.Arrays;

/**
 * 选择类型枚举，区分单选与多选，适用于规格/属性等场景。
 */
public enum SelectType {

    SINGLE(1, "单选"),
    MULTI(2, "多选");

    private final int code;
    private final String description;

    SelectType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static SelectType fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(item -> item.code == code)
                .findFirst()
                .orElse(null);
    }
}
