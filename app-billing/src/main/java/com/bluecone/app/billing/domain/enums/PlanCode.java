package com.bluecone.app.billing.domain.enums;

/**
 * 套餐编码枚举
 */
public enum PlanCode {
    
    FREE("FREE", "免费版", 0),
    BASIC("BASIC", "基础版", 1),
    PRO("PRO", "专业版", 2),
    ENTERPRISE("ENTERPRISE", "企业版", 3);
    
    private final String code;
    private final String name;
    private final int level;
    
    PlanCode(String code, String name, int level) {
        this.code = code;
        this.name = name;
        this.level = level;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getName() {
        return name;
    }
    
    public int getLevel() {
        return level;
    }
    
    public static PlanCode fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (PlanCode planCode : values()) {
            if (planCode.code.equals(code)) {
                return planCode;
            }
        }
        return null;
    }
    
    public boolean isHigherThan(PlanCode other) {
        return this.level > other.level;
    }
    
    public boolean isLowerThan(PlanCode other) {
        return this.level < other.level;
    }
}
