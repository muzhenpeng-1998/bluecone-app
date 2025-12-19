package com.bluecone.app.campaign.domain.repository;

import com.bluecone.app.campaign.api.enums.CampaignStatus;
import com.bluecone.app.campaign.api.enums.CampaignType;
import com.bluecone.app.campaign.domain.model.Campaign;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 活动仓储接口
 */
public interface CampaignRepository {
    
    /**
     * 保存活动
     */
    void save(Campaign campaign);
    
    /**
     * 更新活动
     */
    void update(Campaign campaign);
    
    /**
     * 根据ID查询
     */
    Optional<Campaign> findById(Long tenantId, Long campaignId);
    
    /**
     * 根据编码查询
     */
    Optional<Campaign> findByCode(Long tenantId, String campaignCode);
    
    /**
     * 查询活动列表
     */
    List<Campaign> findByTenant(Long tenantId, CampaignType campaignType);
    
    /**
     * 查询可用活动（按优先级排序）
     * 
     * @param tenantId 租户ID
     * @param campaignType 活动类型
     * @param status 活动状态（通常是 ONLINE）
     * @param queryTime 查询时间（用于时间窗口过滤）
     * @return 活动列表（按 priority DESC 排序）
     */
    List<Campaign> findAvailableCampaigns(Long tenantId, CampaignType campaignType, 
                                          CampaignStatus status, LocalDateTime queryTime);
    
    /**
     * 删除活动（逻辑删除）
     * 
     * @param tenantId 租户ID
     * @param campaignId 活动ID
     */
    void delete(Long tenantId, Long campaignId);
}
