package com.bluecone.app.campaign.api.enums;

/**
 * 活动适用范围枚举
 */
public enum CampaignScope {
    
    /**
     * 全部（租户级）
     */
    ALL("全部"),
    
    /**
     * 指定门店
     */
    STORE("指定门店"),
    
    /**
     * 指定渠道（预留）
     */
    CHANNEL("指定渠道");
    
    private final String description;
    
    CampaignScope(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
