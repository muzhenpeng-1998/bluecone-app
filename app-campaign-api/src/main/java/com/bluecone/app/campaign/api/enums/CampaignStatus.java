package com.bluecone.app.campaign.api.enums;

/**
 * 活动状态枚举
 */
public enum CampaignStatus {
    
    /**
     * 草稿（未发布）
     */
    DRAFT("草稿"),
    
    /**
     * 已上线（进行中）
     */
    ONLINE("已上线"),
    
    /**
     * 已下线（手动停止）
     */
    OFFLINE("已下线"),
    
    /**
     * 已过期（时间窗结束）
     */
    EXPIRED("已过期");
    
    private final String description;
    
    CampaignStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 是否有效（可以被执行）
     */
    public boolean isEffective() {
        return this == ONLINE;
    }
}
