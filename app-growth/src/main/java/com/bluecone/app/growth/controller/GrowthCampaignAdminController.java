package com.bluecone.app.growth.controller;

import com.bluecone.app.core.web.Result;
import com.bluecone.app.growth.api.dto.CampaignDTO;
import com.bluecone.app.growth.api.dto.CreateCampaignRequest;
import com.bluecone.app.growth.api.dto.UpdateCampaignRequest;
import com.bluecone.app.growth.application.CampaignManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 增长引擎 - 活动管理接口（后台）
 */
@RestController
@RequestMapping("/admin/growth/campaigns")
@RequiredArgsConstructor
public class GrowthCampaignAdminController {
    
    private final CampaignManagementService campaignManagementService;
    
    /**
     * 创建活动
     * 
     * POST /admin/growth/campaigns
     */
    @PostMapping
    public Result<CampaignDTO> createCampaign(
            @Valid @RequestBody CreateCampaignRequest request,
            @RequestAttribute Long tenantId) {
        
        CampaignDTO campaign = campaignManagementService.createCampaign(tenantId, request);
        return Result.success(campaign);
    }
    
    /**
     * 更新活动
     * 
     * PUT /admin/growth/campaigns/{campaignCode}
     */
    @PutMapping("/{campaignCode}")
    public Result<CampaignDTO> updateCampaign(
            @PathVariable String campaignCode,
            @Valid @RequestBody UpdateCampaignRequest request,
            @RequestAttribute Long tenantId) {
        
        CampaignDTO campaign = campaignManagementService.updateCampaign(tenantId, campaignCode, request);
        return Result.success(campaign);
    }
    
    /**
     * 获取活动详情
     * 
     * GET /admin/growth/campaigns/{campaignCode}
     */
    @GetMapping("/{campaignCode}")
    public Result<CampaignDTO> getCampaign(
            @PathVariable String campaignCode,
            @RequestAttribute Long tenantId) {
        
        CampaignDTO campaign = campaignManagementService.getCampaign(tenantId, campaignCode);
        return Result.success(campaign);
    }
    
    /**
     * 获取活动列表
     * 
     * GET /admin/growth/campaigns
     */
    @GetMapping
    public Result<List<CampaignDTO>> listCampaigns(@RequestAttribute Long tenantId) {
        List<CampaignDTO> campaigns = campaignManagementService.listCampaigns(tenantId);
        return Result.success(campaigns);
    }
}
