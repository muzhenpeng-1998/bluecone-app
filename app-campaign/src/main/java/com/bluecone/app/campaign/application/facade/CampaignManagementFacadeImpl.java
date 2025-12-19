package com.bluecone.app.campaign.application.facade;

import com.bluecone.app.campaign.api.dto.*;
import com.bluecone.app.campaign.api.enums.CampaignType;
import com.bluecone.app.campaign.api.facade.CampaignManagementFacade;
import com.bluecone.app.campaign.application.CampaignManagementService;
import com.bluecone.app.campaign.domain.model.Campaign;
import com.bluecone.app.campaign.domain.model.ExecutionLog;
import com.bluecone.app.campaign.domain.repository.ExecutionLogRepository;
import com.bluecone.app.campaign.infrastructure.converter.CampaignConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 活动管理门面实现
 */
@Service
@RequiredArgsConstructor
public class CampaignManagementFacadeImpl implements CampaignManagementFacade {
    
    private final CampaignManagementService campaignManagementService;
    private final ExecutionLogRepository executionLogRepository;
    private final CampaignConverter converter;
    
    @Override
    public Long createCampaign(CampaignCreateCommand command) {
        return campaignManagementService.createCampaign(command);
    }
    
    @Override
    public void updateCampaign(CampaignUpdateCommand command) {
        campaignManagementService.updateCampaign(command);
    }
    
    @Override
    public void onlineCampaign(Long tenantId, Long campaignId, Long operatorId) {
        campaignManagementService.onlineCampaign(tenantId, campaignId, operatorId);
    }
    
    @Override
    public void offlineCampaign(Long tenantId, Long campaignId, Long operatorId) {
        campaignManagementService.offlineCampaign(tenantId, campaignId, operatorId);
    }
    
    @Override
    public void deleteCampaign(Long tenantId, Long campaignId, Long operatorId) {
        campaignManagementService.deleteCampaign(tenantId, campaignId, operatorId);
    }
    
    @Override
    public List<CampaignDTO> listCampaigns(Long tenantId, CampaignType campaignType) {
        List<Campaign> campaigns = campaignManagementService.listCampaigns(tenantId, campaignType);
        return campaigns.stream()
                .map(converter::toDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ExecutionLogDTO> listExecutionLogs(Long tenantId, Long campaignId, Long userId, Integer limit) {
        List<ExecutionLog> logs = executionLogRepository.findByConditions(tenantId, campaignId, userId, limit);
        return logs.stream()
                .map(converter::toExecutionLogDTO)
                .collect(Collectors.toList());
    }
}
