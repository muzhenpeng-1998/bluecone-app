package com.bluecone.app.billing.domain.enums;

/**
 * 计费周期枚举
 */
public enum BillingPeriod {
    
    FOREVER("FOREVER", "永久", 999),
    MONTHLY("MONTHLY", "月付", 1),
    QUARTERLY("QUARTERLY", "季付", 3),
    YEARLY("YEARLY", "年付", 12);
    
    private final String code;
    private final String name;
    private final int months;
    
    BillingPeriod(String code, String name, int months) {
        this.code = code;
        this.name = name;
        this.months = months;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getName() {
        return name;
    }
    
    public int getMonths() {
        return months;
    }
    
    public static BillingPeriod fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (BillingPeriod period : values()) {
            if (period.code.equals(code)) {
                return period;
            }
        }
        return null;
    }
}
