package com.bluecone.app.product.domain.enums;

import java.util.Arrays;

/**
 * 菜单场景枚举，对应 bc_store_menu_snapshot.order_scene 字段。
 */
public enum MenuScene {

    DEFAULT("DEFAULT", "默认场景"),
    BREAKFAST("BREAKFAST", "早餐"),
    LUNCH("LUNCH", "午餐"),
    DINNER("DINNER", "晚餐"),
    NIGHT("NIGHT", "夜宵"),
    OTHER("OTHER", "其他场景");

    private final String code;
    private final String description;

    MenuScene(String code, String description) {
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
     * 根据场景编码解析为枚举，不区分大小写，未匹配时返回 OTHER。
     */
    public static MenuScene fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .orElse(OTHER);
    }
}
