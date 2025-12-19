package com.bluecone.app.growth.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 活动规则（奖励配置）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignRules {
    
    /**
     * 邀请人奖励（老客奖励）
     */
    private List<RewardConfig> inviterRewards;
    
    /**
     * 被邀请人奖励（新客奖励）
     */
    private List<RewardConfig> inviteeRewards;
}
