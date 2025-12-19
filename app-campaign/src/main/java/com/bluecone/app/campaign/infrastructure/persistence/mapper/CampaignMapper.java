package com.bluecone.app.campaign.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.campaign.infrastructure.persistence.po.CampaignPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 活动 Mapper
 */
@Mapper
public interface CampaignMapper extends BaseMapper<CampaignPO> {
    
    /**
     * 查询可用活动（按优先级排序）
     * 
     * @param tenantId 租户ID
     * @param campaignType 活动类型
     * @param status 活动状态
     * @param queryTime 查询时间
     * @return 活动列表（按 priority DESC 排序）
     */
    List<CampaignPO> selectAvailableCampaigns(@Param("tenantId") Long tenantId,
                                               @Param("campaignType") String campaignType,
                                               @Param("status") String status,
                                               @Param("queryTime") LocalDateTime queryTime);
}
