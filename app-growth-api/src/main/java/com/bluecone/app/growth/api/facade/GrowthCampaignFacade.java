package com.bluecone.app.growth.api.facade;

import com.bluecone.app.growth.api.dto.CampaignDTO;
import com.bluecone.app.growth.api.dto.CreateCampaignRequest;
import com.bluecone.app.growth.api.dto.UpdateCampaignRequest;

import java.util.List;

/**
 * 增长引擎-活动门面
 * 供管理后台调用的活动管理接口
 */
public interface GrowthCampaignFacade {
    
    /**
     * 创建活动
     * 
     * @param tenantId 租户ID
     * @param request 创建请求
     * @return 活动信息
     */
    CampaignDTO createCampaign(Long tenantId, CreateCampaignRequest request);
    
    /**
     * 更新活动
     * 
     * @param tenantId 租户ID
     * @param campaignCode 活动编码
     * @param request 更新请求
     * @return 活动信息
     */
    CampaignDTO updateCampaign(Long tenantId, String campaignCode, UpdateCampaignRequest request);
    
    /**
     * 获取活动详情
     * 
     * @param tenantId 租户ID
     * @param campaignCode 活动编码
     * @return 活动信息
     */
    CampaignDTO getCampaign(Long tenantId, String campaignCode);
    
    /**
     * 获取活动列表
     * 
     * @param tenantId 租户ID
     * @return 活动列表
     */
    List<CampaignDTO> listCampaigns(Long tenantId);
}
