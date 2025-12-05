package com.bluecone.app.product.domain.enums;

import java.util.Arrays;

/**
 * 售卖渠道枚举，对应商品在门店/渠道维度的配置（如堂食、外卖、自取等）。
 */
public enum SaleChannel {

    ALL("ALL", "全部渠道"),
    DINE_IN("DINE_IN", "堂食"),
    TAKEAWAY("TAKEAWAY", "打包/带走"),
    DELIVERY("DELIVERY", "外卖配送"),
    PICKUP("PICKUP", "自取/到店取"),
    E_COMMERCE("E_COMMERCE", "电商/商城"),
    OTHER("OTHER", "其他渠道");

    private final String code;
    private final String description;

    SaleChannel(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据渠道编码解析为枚举，不区分大小写，未匹配时返回 OTHER。
     */
    public static SaleChannel fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .orElse(OTHER);
    }
}
