package com.bluecone.app.member.domain.enums;

/**
 * 会员状态枚举
 * 
 * @author bluecone
 * @since 2025-12-18
 */
public enum MemberStatus {
    
    /**
     * 正常（可正常使用积分等权益）
     */
    ACTIVE("正常"),
    
    /**
     * 停用（不能使用，但数据保留）
     */
    INACTIVE("停用"),
    
    /**
     * 冻结（涉及违规等，暂停权益）
     */
    FROZEN("冻结");
    
    private final String description;
    
    MemberStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
