package com.bluecone.app.campaign.api.facade;

import com.bluecone.app.campaign.api.dto.CampaignDTO;
import com.bluecone.app.campaign.api.dto.CampaignQueryContext;

import java.util.List;

/**
 * 活动查询门面接口
 * 提供活动查询能力，主要用于计价阶段和事件消费
 */
public interface CampaignQueryFacade {
    
    /**
     * 查询可用活动列表
     * 按优先级（priority DESC）排序
     * 
     * @param context 查询上下文
     * @return 可用活动列表（已按优先级排序）
     */
    List<CampaignDTO> queryAvailableCampaigns(CampaignQueryContext context);
    
    /**
     * 查询单个活动
     * 
     * @param tenantId 租户ID
     * @param campaignId 活动ID
     * @return 活动信息
     */
    CampaignDTO getCampaign(Long tenantId, Long campaignId);
}
