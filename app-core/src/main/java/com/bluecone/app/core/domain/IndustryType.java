package com.bluecone.app.core.domain;

/**
 * 行业类型 / 经营场景枚举，对应门店等业务中的 industryType 字段。
 */
public enum IndustryType {

    /** 咖啡业态，例如精品咖啡馆。 */
    COFFEE,

    /** 餐饮业态（正餐/快餐等）。 */
    FOOD,

    /** 烘焙/烘焙咖啡等业态。 */
    BAKERY,

    /** 其他业态，默认兜底。 */
    OTHER;

    /**
     * 根据字符串解析行业类型，兼容大小写和空值；
     * 未匹配到时返回 {@link #OTHER} 作为兜底。
     */
    public static IndustryType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String normalized = code.trim().toUpperCase();
        for (IndustryType type : values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        return OTHER;
    }
}
