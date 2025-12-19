package com.bluecone.app.campaign.api.facade;

import com.bluecone.app.campaign.api.dto.*;
import com.bluecone.app.campaign.api.enums.CampaignType;

import java.util.List;

/**
 * 活动管理门面接口
 * 提供活动的 CRUD 和上下线操作
 */
public interface CampaignManagementFacade {
    
    /**
     * 创建活动
     * 
     * @param command 创建命令
     * @return 活动ID
     */
    Long createCampaign(CampaignCreateCommand command);
    
    /**
     * 更新活动
     * 
     * @param command 更新命令
     */
    void updateCampaign(CampaignUpdateCommand command);
    
    /**
     * 上线活动
     * 
     * @param tenantId 租户ID
     * @param campaignId 活动ID
     * @param operatorId 操作人ID
     */
    void onlineCampaign(Long tenantId, Long campaignId, Long operatorId);
    
    /**
     * 下线活动
     * 
     * @param tenantId 租户ID
     * @param campaignId 活动ID
     * @param operatorId 操作人ID
     */
    void offlineCampaign(Long tenantId, Long campaignId, Long operatorId);
    
    /**
     * 删除活动（逻辑删除）
     * 
     * @param tenantId 租户ID
     * @param campaignId 活动ID
     * @param operatorId 操作人ID
     */
    void deleteCampaign(Long tenantId, Long campaignId, Long operatorId);
    
    /**
     * 查询活动列表
     * 
     * @param tenantId 租户ID
     * @param campaignType 活动类型（可选）
     * @return 活动列表
     */
    List<CampaignDTO> listCampaigns(Long tenantId, CampaignType campaignType);
    
    /**
     * 查询活动执行日志
     * 
     * @param tenantId 租户ID
     * @param campaignId 活动ID（可选）
     * @param userId 用户ID（可选）
     * @param limit 返回数量限制
     * @return 执行日志列表
     */
    List<ExecutionLogDTO> listExecutionLogs(Long tenantId, Long campaignId, Long userId, Integer limit);
}
