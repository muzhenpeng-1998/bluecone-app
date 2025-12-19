package com.bluecone.app.campaign.application;

import com.bluecone.app.campaign.api.dto.CampaignCreateCommand;
import com.bluecone.app.campaign.api.dto.CampaignUpdateCommand;
import com.bluecone.app.campaign.api.enums.CampaignStatus;
import com.bluecone.app.campaign.api.enums.CampaignType;
import com.bluecone.app.campaign.domain.model.Campaign;
import com.bluecone.app.campaign.domain.repository.CampaignRepository;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.id.api.IdScope;
import com.bluecone.app.id.api.IdService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 活动管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignManagementService {
    
    private final CampaignRepository campaignRepository;
    private final IdService idService;
    
    /**
     * 创建活动
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createCampaign(CampaignCreateCommand command) {
        // 检查编码唯一性
        campaignRepository.findByCode(command.getTenantId(), command.getCampaignCode())
                .ifPresent(c -> {
                    throw new BusinessException("CAMPAIGN_CODE_EXISTS", "活动编码已存在");
                });
        
        Campaign campaign = Campaign.builder()
                .id(idService.nextLong(IdScope.CAMPAIGN))
                .tenantId(command.getTenantId())
                .campaignCode(command.getCampaignCode())
                .campaignName(command.getCampaignName())
                .campaignType(command.getCampaignType())
                .status(CampaignStatus.DRAFT)
                .rules(command.getRules())
                .scope(command.getScope())
                .startTime(command.getStartTime())
                .endTime(command.getEndTime())
                .priority(command.getPriority())
                .description(command.getDescription())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        campaignRepository.save(campaign);
        
        log.info("[campaign-mgmt] 活动创建成功，id={}, code={}", campaign.getId(), campaign.getCampaignCode());
        
        return campaign.getId();
    }
    
    /**
     * 更新活动
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateCampaign(CampaignUpdateCommand command) {
        Campaign campaign = campaignRepository.findById(command.getTenantId(), command.getCampaignId())
                .orElseThrow(() -> new BusinessException("CAMPAIGN_NOT_FOUND", "活动不存在"));
        
        if (!campaign.canUpdate()) {
            throw new BusinessException("CAMPAIGN_CANNOT_UPDATE", "活动状态不允许修改");
        }
        
        // 更新字段
        if (command.getCampaignName() != null) {
            campaign.setCampaignName(command.getCampaignName());
        }
        if (command.getRules() != null) {
            campaign.setRules(command.getRules());
        }
        if (command.getScope() != null) {
            campaign.setScope(command.getScope());
        }
        if (command.getStartTime() != null) {
            campaign.setStartTime(command.getStartTime());
        }
        if (command.getEndTime() != null) {
            campaign.setEndTime(command.getEndTime());
        }
        if (command.getPriority() != null) {
            campaign.setPriority(command.getPriority());
        }
        if (command.getDescription() != null) {
            campaign.setDescription(command.getDescription());
        }
        
        campaign.setUpdatedAt(LocalDateTime.now());
        campaignRepository.update(campaign);
        
        log.info("[campaign-mgmt] 活动更新成功，id={}", campaign.getId());
    }
    
    /**
     * 上线活动
     */
    @Transactional(rollbackFor = Exception.class)
    public void onlineCampaign(Long tenantId, Long campaignId, Long operatorId) {
        Campaign campaign = campaignRepository.findById(tenantId, campaignId)
                .orElseThrow(() -> new BusinessException("CAMPAIGN_NOT_FOUND", "活动不存在"));
        
        campaign.online();
        campaign.setUpdatedAt(LocalDateTime.now());
        campaignRepository.update(campaign);
        
        log.info("[campaign-mgmt] 活动上线成功，id={}, operator={}", campaignId, operatorId);
    }
    
    /**
     * 下线活动
     */
    @Transactional(rollbackFor = Exception.class)
    public void offlineCampaign(Long tenantId, Long campaignId, Long operatorId) {
        Campaign campaign = campaignRepository.findById(tenantId, campaignId)
                .orElseThrow(() -> new BusinessException("CAMPAIGN_NOT_FOUND", "活动不存在"));
        
        campaign.offline();
        campaign.setUpdatedAt(LocalDateTime.now());
        campaignRepository.update(campaign);
        
        log.info("[campaign-mgmt] 活动下线成功，id={}, operator={}", campaignId, operatorId);
    }
    
    /**
     * 删除活动（逻辑删除）
     * 只能删除草稿或已下线的活动
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteCampaign(Long tenantId, Long campaignId, Long operatorId) {
        Campaign campaign = campaignRepository.findById(tenantId, campaignId)
                .orElseThrow(() -> new BusinessException("CAMPAIGN_NOT_FOUND", "活动不存在"));
        
        // 状态检查：只能删除草稿或已下线的活动
        if (campaign.getStatus() == CampaignStatus.ONLINE) {
            throw new BusinessException("CAMPAIGN_CANNOT_DELETE", "活动已上线，不能删除，请先下线");
        }
        
        // 逻辑删除
        campaignRepository.delete(tenantId, campaignId);
        
        log.info("[campaign-mgmt] 活动删除成功，id={}, code={}, operator={}", 
                campaignId, campaign.getCampaignCode(), operatorId);
    }
    
    /**
     * 查询活动列表
     */
    public List<Campaign> listCampaigns(Long tenantId, CampaignType campaignType) {
        return campaignRepository.findByTenant(tenantId, campaignType);
    }
}
