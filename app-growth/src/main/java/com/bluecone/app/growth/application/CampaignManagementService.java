package com.bluecone.app.growth.application;

import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.growth.api.dto.CampaignDTO;
import com.bluecone.app.growth.api.dto.CampaignRules;
import com.bluecone.app.growth.api.dto.CreateCampaignRequest;
import com.bluecone.app.growth.api.dto.UpdateCampaignRequest;
import com.bluecone.app.growth.api.enums.CampaignStatus;
import com.bluecone.app.growth.domain.model.GrowthCampaign;
import com.bluecone.app.growth.domain.repository.GrowthCampaignRepository;
import com.bluecone.app.id.api.IdScope;
import com.bluecone.app.id.api.IdService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 活动管理服务
 */
@Slf4j
@Service("growthCampaignManagementService")
public class CampaignManagementService {
    
    private final GrowthCampaignRepository campaignRepository;
    private final IdService idService;
    private final ObjectMapper objectMapper;

    public CampaignManagementService(GrowthCampaignRepository campaignRepository,
                                    IdService idService,
                                    @Qualifier("redisObjectMapper") ObjectMapper objectMapper) {
        this.campaignRepository = campaignRepository;
        this.idService = idService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * 创建活动
     */
    @Transactional
    public CampaignDTO createCampaign(Long tenantId, CreateCampaignRequest request) {
        // 1. 检查活动编码是否已存在
        campaignRepository.findByCode(tenantId, request.getCampaignCode())
                .ifPresent(c -> {
                    throw new BusinessException("CAMPAIGN_CODE_EXISTS", "活动编码已存在");
                });
        
        // 2. 序列化规则
        String rulesJson;
        try {
            rulesJson = objectMapper.writeValueAsString(request.getRules());
        } catch (JsonProcessingException e) {
            throw new BusinessException("INVALID_RULES", "规则格式无效");
        }
        
        // 3. 创建活动
        GrowthCampaign campaign = GrowthCampaign.builder()
                .id(idService.nextLong(IdScope.GROWTH))
                .tenantId(tenantId)
                .campaignCode(request.getCampaignCode())
                .campaignName(request.getCampaignName())
                .campaignType(request.getCampaignType())
                .status(CampaignStatus.DRAFT)
                .rulesJson(rulesJson)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .description(request.getDescription())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        campaignRepository.save(campaign);
        
        log.info("创建活动成功: tenantId={}, campaignCode={}", tenantId, request.getCampaignCode());
        
        return toDTO(campaign, request.getRules());
    }
    
    /**
     * 更新活动
     */
    @Transactional
    public CampaignDTO updateCampaign(Long tenantId, String campaignCode, UpdateCampaignRequest request) {
        // 1. 查询活动
        GrowthCampaign campaign = campaignRepository.findByCode(tenantId, campaignCode)
                .orElseThrow(() -> new BusinessException("CAMPAIGN_NOT_FOUND", "活动不存在"));
        
        // 2. 检查是否可以修改
        if (!campaign.canUpdate()) {
            throw new BusinessException("CAMPAIGN_CANNOT_UPDATE", 
                    "活动状态不允许修改，当前状态: " + campaign.getStatus());
        }
        
        // 3. 更新字段
        if (request.getCampaignName() != null) {
            campaign.setCampaignName(request.getCampaignName());
        }
        if (request.getStatus() != null) {
            campaign.setStatus(request.getStatus());
        }
        if (request.getRules() != null) {
            try {
                String rulesJson = objectMapper.writeValueAsString(request.getRules());
                campaign.setRulesJson(rulesJson);
            } catch (JsonProcessingException e) {
                throw new BusinessException("INVALID_RULES", "规则格式无效");
            }
        }
        if (request.getStartTime() != null) {
            campaign.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            campaign.setEndTime(request.getEndTime());
        }
        if (request.getDescription() != null) {
            campaign.setDescription(request.getDescription());
        }
        campaign.setUpdatedAt(LocalDateTime.now());
        
        campaignRepository.update(campaign);
        
        log.info("更新活动成功: tenantId={}, campaignCode={}", tenantId, campaignCode);
        
        CampaignRules rules = request.getRules();
        if (rules == null) {
            try {
                rules = objectMapper.readValue(campaign.getRulesJson(), CampaignRules.class);
            } catch (JsonProcessingException e) {
                rules = null;
            }
        }
        
        return toDTO(campaign, rules);
    }
    
    /**
     * 获取活动详情
     */
    public CampaignDTO getCampaign(Long tenantId, String campaignCode) {
        GrowthCampaign campaign = campaignRepository.findByCode(tenantId, campaignCode)
                .orElseThrow(() -> new BusinessException("CAMPAIGN_NOT_FOUND", "活动不存在"));
        
        CampaignRules rules = null;
        try {
            rules = objectMapper.readValue(campaign.getRulesJson(), CampaignRules.class);
        } catch (JsonProcessingException e) {
            log.error("解析活动规则失败: campaignCode={}", campaignCode, e);
        }
        
        return toDTO(campaign, rules);
    }
    
    /**
     * 获取活动列表
     */
    public List<CampaignDTO> listCampaigns(Long tenantId) {
        List<GrowthCampaign> campaigns = campaignRepository.findByTenantId(tenantId);
        
        return campaigns.stream()
                .map(campaign -> {
                    CampaignRules rules = null;
                    try {
                        rules = objectMapper.readValue(campaign.getRulesJson(), CampaignRules.class);
                    } catch (JsonProcessingException e) {
                        log.error("解析活动规则失败: campaignCode={}", campaign.getCampaignCode(), e);
                    }
                    return toDTO(campaign, rules);
                })
                .collect(Collectors.toList());
    }
    
    private CampaignDTO toDTO(GrowthCampaign campaign, CampaignRules rules) {
        return CampaignDTO.builder()
                .id(campaign.getId())
                .tenantId(campaign.getTenantId())
                .campaignCode(campaign.getCampaignCode())
                .campaignName(campaign.getCampaignName())
                .campaignType(campaign.getCampaignType())
                .status(campaign.getStatus())
                .rules(rules)
                .startTime(campaign.getStartTime())
                .endTime(campaign.getEndTime())
                .description(campaign.getDescription())
                .createdAt(campaign.getCreatedAt())
                .updatedAt(campaign.getUpdatedAt())
                .build();
    }
}
