package com.bluecone.app.product.domain.enums;

import java.util.Arrays;

/**
 * 商品及相关配置的通用状态枚举，映射数据库中的状态字段。
 * <p>约定：0=草稿，1=启用，-1=禁用/删除。</p>
 */
public enum ProductStatus {

    DRAFT(0, "草稿"),
    ENABLED(1, "启用"),
    DISABLED(-1, "禁用");

    private final int code;
    private final String description;

    ProductStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据数据库中的整数状态码解析为枚举，未匹配时返回 null。
     */
    public static ProductStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(item -> item.code == code)
                .findFirst()
                .orElse(null);
    }
}
