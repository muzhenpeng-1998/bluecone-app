package com.bluecone.app.product.domain.enums;

import java.util.Arrays;

/**
 * 商品类型枚举，对应 bc_product.product_type 字段。
 */
public enum ProductType {

    FOOD_DRINK(1, "餐饮/饮品"),
    SERVICE(2, "服务"),
    VENUE_TEMPLATE(3, "场馆模板"),
    STORED_VALUE(4, "储值类商品"),
    MEMBERSHIP(5, "会员开通类商品"),
    COUPON_PACKAGE(6, "优惠券包/权益包");

    private final int code;
    private final String description;

    ProductType(int code, String description) {
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
     * 根据数据库中的类型值解析为枚举，未匹配时返回 null。
     */
    public static ProductType fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(item -> item.code == code)
                .findFirst()
                .orElse(null);
    }
}
