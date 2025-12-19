package com.bluecone.app.growth.domain.repository;

import com.bluecone.app.growth.domain.model.GrowthCampaign;

import java.util.List;
import java.util.Optional;

/**
 * 增长活动仓储接口
 */
public interface GrowthCampaignRepository {
    
    /**
     * 保存活动
     */
    void save(GrowthCampaign campaign);
    
    /**
     * 更新活动
     */
    void update(GrowthCampaign campaign);
    
    /**
     * 根据租户ID和活动编码查询
     */
    Optional<GrowthCampaign> findByCode(Long tenantId, String campaignCode);
    
    /**
     * 根据租户ID查询活动列表
     */
    List<GrowthCampaign> findByTenantId(Long tenantId);
    
    /**
     * 根据ID查询
     */
    Optional<GrowthCampaign> findById(Long id);
}
