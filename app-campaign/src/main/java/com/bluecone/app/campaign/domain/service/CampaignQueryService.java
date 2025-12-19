package com.bluecone.app.campaign.domain.service;

import com.bluecone.app.campaign.api.dto.CampaignQueryContext;
import com.bluecone.app.campaign.api.dto.CampaignScopeDTO;
import com.bluecone.app.campaign.api.enums.CampaignScope;
import com.bluecone.app.campaign.api.enums.CampaignStatus;
import com.bluecone.app.campaign.domain.model.Campaign;
import com.bluecone.app.campaign.domain.repository.CampaignRepository;
import com.bluecone.app.campaign.domain.repository.ExecutionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 活动查询服务
 * 提供活动查询和匹配逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignQueryService {
    
    private final CampaignRepository campaignRepository;
    private final ExecutionLogRepository executionLogRepository;
    
    /**
     * 查询可用活动列表
     * 按优先级排序，并过滤适用范围和用户参与次数
     * 
     * @param context 查询上下文
     * @return 可用活动列表
     */
    public List<Campaign> queryAvailableCampaigns(CampaignQueryContext context) {
        // 1. 查询所有有效活动（按优先级排序）
        List<Campaign> campaigns = campaignRepository.findAvailableCampaigns(
                context.getTenantId(),
                context.getCampaignType(),
                CampaignStatus.ONLINE,
                context.getQueryTime()
        );
        
        log.debug("[campaign-query] 查询到 {} 个有效活动，tenantId={}, type={}", 
                campaigns.size(), context.getTenantId(), context.getCampaignType());
        
        // 2. 过滤适用范围和用户参与次数
        return campaigns.stream()
                .filter(campaign -> matchScope(campaign, context))
                .filter(campaign -> matchUserLimit(campaign, context))
                .filter(campaign -> matchAmountThreshold(campaign, context))
                .collect(Collectors.toList());
    }
    
    /**
     * 匹配活动适用范围
     */
    private boolean matchScope(Campaign campaign, CampaignQueryContext context) {
        CampaignScopeDTO scope = campaign.getScope();
        if (scope == null || scope.getScopeType() == null) {
            return true;
        }
        
        if (scope.getScopeType() == CampaignScope.ALL) {
            return true;
        }
        
        if (scope.getScopeType() == CampaignScope.STORE) {
            if (context.getStoreId() == null) {
                return false;
            }
            return scope.getStoreIds() != null && scope.getStoreIds().contains(context.getStoreId());
        }
        
        if (scope.getScopeType() == CampaignScope.CHANNEL) {
            if (context.getChannel() == null) {
                return false;
            }
            return scope.getChannels() != null && scope.getChannels().contains(context.getChannel());
        }
        
        return false;
    }
    
    /**
     * 匹配用户参与次数限制
     */
    private boolean matchUserLimit(Campaign campaign, CampaignQueryContext context) {
        if (campaign.getRules() == null || campaign.getRules().getPerUserLimit() == null) {
            return true;
        }
        
        if (context.getUserId() == null) {
            // 如果没有用户ID，无法校验次数，跳过此活动
            log.warn("[campaign-query] 活动 {} 有用户次数限制，但查询上下文无用户ID，跳过", campaign.getCampaignCode());
            return false;
        }
        
        Integer limit = campaign.getRules().getPerUserLimit();
        int executedCount = executionLogRepository.countUserExecutions(
                context.getTenantId(),
                campaign.getId(),
                context.getUserId()
        );
        
        boolean match = executedCount < limit;
        if (!match) {
            log.debug("[campaign-query] 活动 {} 用户 {} 已达参与次数上限 {}/{}", 
                    campaign.getCampaignCode(), context.getUserId(), executedCount, limit);
        }
        return match;
    }
    
    /**
     * 匹配金额门槛
     */
    private boolean matchAmountThreshold(Campaign campaign, CampaignQueryContext context) {
        if (campaign.getRules() == null || campaign.getRules().getMinAmount() == null) {
            return true;
        }
        
        if (context.getAmount() == null) {
            return true;
        }
        
        BigDecimal minAmount = campaign.getRules().getMinAmount();
        boolean match = context.getAmount().compareTo(minAmount) >= 0;
        
        if (!match) {
            log.debug("[campaign-query] 活动 {} 金额门槛不满足，当前={}, 最低={}", 
                    campaign.getCampaignCode(), context.getAmount(), minAmount);
        }
        return match;
    }
}
