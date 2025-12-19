package com.bluecone.app.campaign.api.dto;

import com.bluecone.app.campaign.api.enums.CampaignScope;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 活动适用范围 DTO（JSON 序列化）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignScopeDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 范围类型：ALL-全部, STORE-指定门店, CHANNEL-指定渠道
     */
    private CampaignScope scopeType;
    
    /**
     * 门店ID列表（当 scopeType = STORE 时使用）
     */
    private List<Long> storeIds;
    
    /**
     * 渠道列表（当 scopeType = CHANNEL 时使用，预留）
     */
    private List<String> channels;
}
