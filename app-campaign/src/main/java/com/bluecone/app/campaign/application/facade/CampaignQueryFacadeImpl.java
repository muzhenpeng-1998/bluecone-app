package com.bluecone.app.campaign.application.facade;

import com.bluecone.app.campaign.api.dto.CampaignDTO;
import com.bluecone.app.campaign.api.dto.CampaignQueryContext;
import com.bluecone.app.campaign.api.facade.CampaignQueryFacade;
import com.bluecone.app.campaign.domain.model.Campaign;
import com.bluecone.app.campaign.domain.repository.CampaignRepository;
import com.bluecone.app.campaign.domain.service.CampaignQueryService;
import com.bluecone.app.campaign.infrastructure.converter.CampaignConverter;
import com.bluecone.app.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 活动查询门面实现
 */
@Service
@RequiredArgsConstructor
public class CampaignQueryFacadeImpl implements CampaignQueryFacade {
    
    private final CampaignQueryService campaignQueryService;
    private final CampaignRepository campaignRepository;
    private final CampaignConverter converter;
    
    @Override
    public List<CampaignDTO> queryAvailableCampaigns(CampaignQueryContext context) {
        List<Campaign> campaigns = campaignQueryService.queryAvailableCampaigns(context);
        return campaigns.stream()
                .map(converter::toDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    public CampaignDTO getCampaign(Long tenantId, Long campaignId) {
        Campaign campaign = campaignRepository.findById(tenantId, campaignId)
                .orElseThrow(() -> new BusinessException("CAMPAIGN_NOT_FOUND", "活动不存在"));
        return converter.toDTO(campaign);
    }
}
