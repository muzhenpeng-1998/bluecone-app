package com.bluecone.app.api.admin.marketing;

import com.bluecone.app.campaign.api.dto.*;
import com.bluecone.app.campaign.api.enums.CampaignType;
import com.bluecone.app.campaign.api.facade.CampaignManagementFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 活动管理后台接口
 */
@Slf4j
@RestController
@RequestMapping("/admin/campaigns")
@RequiredArgsConstructor
@Tag(name = "🎛️ 平台管理后台 > 营销管理 > 活动管理", description = "活动配置、上下线、执行日志查询")
public class CampaignAdminController {
    
    private final CampaignManagementFacade campaignManagementFacade;
    
    /**
     * 创建活动
     */
    @PostMapping
    @Operation(summary = "创建活动")
    public Long createCampaign(@Valid @RequestBody CampaignCreateCommand command) {
        log.info("[admin-campaign] 创建活动，code={}, type={}", 
                command.getCampaignCode(), command.getCampaignType());
        
        return campaignManagementFacade.createCampaign(command);
    }
    
    /**
     * 更新活动
     */
    @PutMapping("/{campaignId}")
    @Operation(summary = "更新活动")
    public void updateCampaign(
            @PathVariable Long campaignId,
            @RequestParam Long tenantId,
            @Valid @RequestBody CampaignUpdateCommand command) {
        
        log.info("[admin-campaign] 更新活动，id={}", campaignId);
        
        command.setTenantId(tenantId);
        command.setCampaignId(campaignId);
        campaignManagementFacade.updateCampaign(command);
    }
    
    /**
     * 上线活动
     */
    @PostMapping("/{campaignId}/online")
    @Operation(summary = "上线活动")
    public void onlineCampaign(
            @PathVariable Long campaignId,
            @RequestParam Long tenantId,
            @RequestParam Long operatorId) {
        
        log.info("[admin-campaign] 上线活动，id={}, operator={}", campaignId, operatorId);
        
        campaignManagementFacade.onlineCampaign(tenantId, campaignId, operatorId);
    }
    
    /**
     * 下线活动
     */
    @PostMapping("/{campaignId}/offline")
    @Operation(summary = "下线活动")
    public void offlineCampaign(
            @PathVariable Long campaignId,
            @RequestParam Long tenantId,
            @RequestParam Long operatorId) {
        
        log.info("[admin-campaign] 下线活动，id={}, operator={}", campaignId, operatorId);
        
        campaignManagementFacade.offlineCampaign(tenantId, campaignId, operatorId);
    }
    
    /**
     * 删除活动
     */
    @DeleteMapping("/{campaignId}")
    @Operation(summary = "删除活动（逻辑删除）")
    public void deleteCampaign(
            @PathVariable Long campaignId,
            @RequestParam Long tenantId,
            @RequestParam Long operatorId) {
        
        log.info("[admin-campaign] 删除活动，id={}, operator={}", campaignId, operatorId);
        
        campaignManagementFacade.deleteCampaign(tenantId, campaignId, operatorId);
    }
    
    /**
     * 查询活动列表
     */
    @GetMapping
    @Operation(summary = "查询活动列表")
    public List<CampaignDTO> listCampaigns(
            @RequestParam Long tenantId,
            @RequestParam(required = false) CampaignType campaignType) {
        
        log.info("[admin-campaign] 查询活动列表，tenantId={}, type={}", tenantId, campaignType);
        
        return campaignManagementFacade.listCampaigns(tenantId, campaignType);
    }
    
    /**
     * 查询活动执行日志
     */
    @GetMapping("/execution-logs")
    @Operation(summary = "查询活动执行日志")
    public List<ExecutionLogDTO> listExecutionLogs(
            @RequestParam Long tenantId,
            @RequestParam(required = false) Long campaignId,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "100") Integer limit) {
        
        log.info("[admin-campaign] 查询执行日志，tenantId={}, campaignId={}, userId={}", 
                tenantId, campaignId, userId);
        
        return campaignManagementFacade.listExecutionLogs(
                tenantId, campaignId, userId, limit
        );
    }
}
