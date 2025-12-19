package com.bluecone.app.growth.application;

import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.growth.api.dto.*;
import com.bluecone.app.growth.api.enums.AttributionStatus;
import com.bluecone.app.growth.api.enums.UserRole;
import com.bluecone.app.growth.domain.model.Attribution;
import com.bluecone.app.growth.domain.model.GrowthCampaign;
import com.bluecone.app.growth.domain.model.InviteCode;
import com.bluecone.app.growth.domain.repository.AttributionRepository;
import com.bluecone.app.growth.domain.repository.GrowthCampaignRepository;
import com.bluecone.app.growth.domain.repository.InviteCodeRepository;
import com.bluecone.app.growth.domain.service.GrowthMetrics;
import com.bluecone.app.growth.domain.service.InviteCodeGenerator;
import com.bluecone.app.growth.domain.service.RewardIssuanceService;
import com.bluecone.app.id.api.IdScope;
import com.bluecone.app.id.api.IdService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 增长引擎应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GrowthApplicationService {
    
    private final GrowthCampaignRepository campaignRepository;
    private final InviteCodeRepository inviteCodeRepository;
    private final AttributionRepository attributionRepository;
    private final RewardIssuanceService rewardIssuanceService;
    private final InviteCodeGenerator inviteCodeGenerator;
    private final IdService idService;
    private final ObjectMapper objectMapper;
    private final GrowthMetrics metrics;
    
    /**
     * 获取或创建邀请码
     */
    @Transactional
    public InviteCodeResponse getOrCreateInviteCode(Long tenantId, Long userId, String campaignCode) {
        // 1. 检查活动是否存在且有效
        GrowthCampaign campaign = campaignRepository.findByCode(tenantId, campaignCode)
                .orElseThrow(() -> new BusinessException("CAMPAIGN_NOT_FOUND", "活动不存在"));
        
        if (!campaign.isActive()) {
            throw new BusinessException("CAMPAIGN_NOT_ACTIVE", "活动未进行中");
        }
        
        // 2. 查询是否已有邀请码
        Optional<InviteCode> existing = inviteCodeRepository.findByInviter(tenantId, campaignCode, userId);
        if (existing.isPresent()) {
            InviteCode code = existing.get();
            return buildInviteCodeResponse(code, campaignCode);
        }
        
        // 3. 生成新邀请码
        String code = inviteCodeGenerator.generate(tenantId, campaignCode, userId);
        InviteCode inviteCode = InviteCode.builder()
                .id(idService.nextLong(IdScope.GROWTH))
                .tenantId(tenantId)
                .campaignCode(campaignCode)
                .inviteCode(code)
                .inviterUserId(userId)
                .invitesCount(0)
                .successfulInvitesCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        try {
            inviteCodeRepository.save(inviteCode);
        } catch (DuplicateKeyException e) {
            // 并发创建，重新查询
            inviteCode = inviteCodeRepository.findByInviter(tenantId, campaignCode, userId)
                    .orElseThrow(() -> new BusinessException("INVITE_CODE_CREATE_FAILED", "邀请码创建失败"));
        }
        
        return buildInviteCodeResponse(inviteCode, campaignCode);
    }
    
    /**
     * 绑定邀请码（新客绑定归因）
     */
    @Transactional
    public BindInviteResponse bindInviteCode(Long tenantId, Long userId, BindInviteRequest request) {
        // 1. 查询邀请码
        InviteCode inviteCode = inviteCodeRepository.findByInviteCode(request.getInviteCode())
                .orElseThrow(() -> new BusinessException("INVITE_CODE_NOT_FOUND", "邀请码不存在"));
        
        // 2. 检查活动是否有效
        GrowthCampaign campaign = campaignRepository.findByCode(tenantId, inviteCode.getCampaignCode())
                .orElseThrow(() -> new BusinessException("CAMPAIGN_NOT_FOUND", "活动不存在"));
        
        if (!campaign.isActive()) {
            throw new BusinessException("CAMPAIGN_NOT_ACTIVE", "活动未进行中");
        }
        
        // 3. 反作弊检查：邀请人不能是自己
        if (inviteCode.getInviterUserId().equals(userId)) {
            throw new BusinessException("SELF_INVITE_NOT_ALLOWED", "不能邀请自己");
        }
        
        // 4. 检查是否已绑定过该活动
        Optional<Attribution> existingAttribution = attributionRepository
                .findByInvitee(tenantId, inviteCode.getCampaignCode(), userId);
        if (existingAttribution.isPresent()) {
            Attribution attr = existingAttribution.get();
            return BindInviteResponse.builder()
                    .success(true)
                    .attributionId(attr.getId())
                    .campaignCode(attr.getCampaignCode())
                    .inviteCode(attr.getInviteCode())
                    .message("已绑定过该活动")
                    .build();
        }
        
        // 5. 创建归因关系
        Attribution attribution = Attribution.builder()
                .id(idService.nextLong(IdScope.GROWTH))
                .tenantId(tenantId)
                .campaignCode(inviteCode.getCampaignCode())
                .inviteCode(request.getInviteCode())
                .inviterUserId(inviteCode.getInviterUserId())
                .inviteeUserId(userId)
                .status(AttributionStatus.PENDING)
                .boundAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        try {
            attributionRepository.save(attribution);
            
            // 6. 更新邀请码的邀请计数
            inviteCodeRepository.incrementInvitesCount(inviteCode.getId());
            
            // 7. 记录指标
            metrics.recordBind();
            
        } catch (DuplicateKeyException e) {
            // 并发绑定，重新查询
            attribution = attributionRepository.findByInvitee(tenantId, inviteCode.getCampaignCode(), userId)
                    .orElseThrow(() -> new BusinessException("ATTRIBUTION_CREATE_FAILED", "归因创建失败"));
        }
        
        return BindInviteResponse.builder()
                .success(true)
                .attributionId(attribution.getId())
                .campaignCode(attribution.getCampaignCode())
                .inviteCode(attribution.getInviteCode())
                .message("绑定成功")
                .build();
    }
    
    /**
     * 处理首单完成事件（触发奖励发放）
     */
    @Transactional
    public void handleFirstOrderCompleted(Long tenantId, Long userId, Long orderId) {
        log.info("处理首单完成事件: tenantId={}, userId={}, orderId={}", tenantId, userId, orderId);
        
        // 1. 检查是否首单
        int paidOrdersCount = attributionRepository.countPaidOrdersByUser(tenantId, userId);
        if (paidOrdersCount != 1) {
            log.info("非首单，跳过奖励发放: userId={}, paidOrdersCount={}", userId, paidOrdersCount);
            return;
        }
        
        // 2. 查询该用户的归因关系（PENDING状态）
        // 注意：一个用户在同一租户可能参与多个活动，需要处理所有PENDING的归因
        // 这里简化处理，假设只有一个活动。实际应该查询所有PENDING归因
        List<GrowthCampaign> campaigns = campaignRepository.findByTenantId(tenantId);
        for (GrowthCampaign campaign : campaigns) {
            Optional<Attribution> attrOpt = attributionRepository.findByInvitee(tenantId, campaign.getCampaignCode(), userId);
            if (attrOpt.isEmpty()) {
                continue;
            }
            
            Attribution attribution = attrOpt.get();
            if (attribution.getStatus() != AttributionStatus.PENDING) {
                continue;
            }
            
            // 3. 确认归因
            attribution.confirm(orderId);
            attributionRepository.update(attribution);
            
            // 4. 更新邀请码的成功邀请计数
            InviteCode inviteCode = inviteCodeRepository.findByInviteCode(attribution.getInviteCode())
                    .orElse(null);
            if (inviteCode != null) {
                inviteCodeRepository.incrementSuccessfulInvitesCount(inviteCode.getId());
            }
            
            // 5. 解析奖励规则并发放奖励
            try {
                CampaignRules rules = objectMapper.readValue(campaign.getRulesJson(), CampaignRules.class);
                
                // 5.1 发放被邀请人奖励
                if (rules.getInviteeRewards() != null) {
                    for (RewardConfig reward : rules.getInviteeRewards()) {
                        rewardIssuanceService.issueReward(tenantId, campaign.getCampaignCode(),
                                attribution.getId(), userId, UserRole.INVITEE, reward, orderId);
                    }
                }
                
                // 5.2 发放邀请人奖励
                if (rules.getInviterRewards() != null) {
                    for (RewardConfig reward : rules.getInviterRewards()) {
                        rewardIssuanceService.issueReward(tenantId, campaign.getCampaignCode(),
                                attribution.getId(), attribution.getInviterUserId(), UserRole.INVITER, reward, orderId);
                    }
                }
                
            } catch (JsonProcessingException e) {
                log.error("解析活动规则失败: campaignCode={}", campaign.getCampaignCode(), e);
            }
        }
    }
    
    private InviteCodeResponse buildInviteCodeResponse(InviteCode inviteCode, String campaignCode) {
        // TODO: 构建完整的邀请链接（需要前端H5页面URL）
        String inviteLink = String.format("https://app.bluecone.com/invite?code=%s&campaign=%s",
                inviteCode.getInviteCode(), campaignCode);
        
        return InviteCodeResponse.builder()
                .inviteCode(inviteCode.getInviteCode())
                .campaignCode(campaignCode)
                .inviteLink(inviteLink)
                .invitesCount(inviteCode.getInvitesCount())
                .successfulInvitesCount(inviteCode.getSuccessfulInvitesCount())
                .build();
    }
}
