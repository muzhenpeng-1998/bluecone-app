package com.bluecone.app.billing.domain.enums;

/**
 * 提醒类型枚举
 */
public enum ReminderType {
    
    /**
     * 到期前7天提醒
     */
    EXPIRE_7D("EXPIRE_7D", "到期前7天提醒", 7),
    
    /**
     * 到期前3天提醒
     */
    EXPIRE_3D("EXPIRE_3D", "到期前3天提醒", 3),
    
    /**
     * 到期前1天提醒
     */
    EXPIRE_1D("EXPIRE_1D", "到期前1天提醒", 1),
    
    /**
     * 到期当天提醒
     */
    EXPIRE_0D("EXPIRE_0D", "到期当天提醒", 0),
    
    /**
     * 宽限期第3天提醒（最后警告）
     */
    GRACE_3D("GRACE_3D", "宽限期第3天提醒", -3);
    
    private final String code;
    private final String name;
    private final int daysBeforeExpire;
    
    ReminderType(String code, String name, int daysBeforeExpire) {
        this.code = code;
        this.name = name;
        this.daysBeforeExpire = daysBeforeExpire;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getName() {
        return name;
    }
    
    public int getDaysBeforeExpire() {
        return daysBeforeExpire;
    }
    
    public static ReminderType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ReminderType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
    
    /**
     * 是否是到期前提醒（用于判断是否需要生成任务）
     */
    public boolean isBeforeExpire() {
        return daysBeforeExpire >= 0;
    }
    
    /**
     * 是否是宽限期提醒
     */
    public boolean isGracePeriod() {
        return daysBeforeExpire < 0;
    }
}
