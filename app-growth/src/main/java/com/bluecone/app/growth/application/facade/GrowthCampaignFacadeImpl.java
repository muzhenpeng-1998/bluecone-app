package com.bluecone.app.growth.application.facade;

import com.bluecone.app.growth.api.dto.CampaignDTO;
import com.bluecone.app.growth.api.dto.CreateCampaignRequest;
import com.bluecone.app.growth.api.dto.UpdateCampaignRequest;
import com.bluecone.app.growth.api.facade.GrowthCampaignFacade;
import com.bluecone.app.growth.application.CampaignManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 增长引擎-活动门面实现
 */
@Service
@RequiredArgsConstructor
public class GrowthCampaignFacadeImpl implements GrowthCampaignFacade {
    
    private final CampaignManagementService campaignManagementService;
    
    @Override
    public CampaignDTO createCampaign(Long tenantId, CreateCampaignRequest request) {
        return campaignManagementService.createCampaign(tenantId, request);
    }
    
    @Override
    public CampaignDTO updateCampaign(Long tenantId, String campaignCode, UpdateCampaignRequest request) {
        return campaignManagementService.updateCampaign(tenantId, campaignCode, request);
    }
    
    @Override
    public CampaignDTO getCampaign(Long tenantId, String campaignCode) {
        return campaignManagementService.getCampaign(tenantId, campaignCode);
    }
    
    @Override
    public List<CampaignDTO> listCampaigns(Long tenantId) {
        return campaignManagementService.listCampaigns(tenantId);
    }
}
