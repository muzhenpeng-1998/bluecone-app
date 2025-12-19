package com.bluecone.app.campaign.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.campaign.infrastructure.persistence.po.ExecutionLogPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 活动执行日志 Mapper
 */
@Mapper
public interface ExecutionLogMapper extends BaseMapper<ExecutionLogPO> {
    
    /**
     * 统计用户在某个活动的执行次数（只统计成功的）
     * 
     * @param tenantId 租户ID
     * @param campaignId 活动ID
     * @param userId 用户ID
     * @return 执行次数
     */
    int countUserSuccessExecutions(@Param("tenantId") Long tenantId,
                                    @Param("campaignId") Long campaignId,
                                    @Param("userId") Long userId);
}
