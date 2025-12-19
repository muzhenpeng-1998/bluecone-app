package com.bluecone.app.growth.controller;

import com.bluecone.app.core.web.Result;
import com.bluecone.app.growth.api.dto.BindInviteRequest;
import com.bluecone.app.growth.api.dto.BindInviteResponse;
import com.bluecone.app.growth.api.dto.InviteCodeResponse;
import com.bluecone.app.growth.application.GrowthApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 增长引擎 - 邀请接口
 */
@RestController
@RequestMapping("/api/growth/invite")
@RequiredArgsConstructor
public class GrowthInviteController {
    
    private final GrowthApplicationService growthApplicationService;
    
    /**
     * 获取或生成邀请码
     * 
     * GET /api/growth/invite?campaignCode=INVITE_2025
     */
    @GetMapping
    public Result<InviteCodeResponse> getInviteCode(
            @RequestParam String campaignCode,
            @RequestAttribute Long tenantId,
            @RequestAttribute Long userId) {
        
        InviteCodeResponse response = growthApplicationService
                .getOrCreateInviteCode(tenantId, userId, campaignCode);
        return Result.success(response);
    }
    
    /**
     * 绑定邀请码（新客绑定归因）
     * 
     * POST /api/growth/bind
     */
    @PostMapping("/bind")
    public Result<BindInviteResponse> bindInviteCode(
            @Valid @RequestBody BindInviteRequest request,
            @RequestAttribute Long tenantId,
            @RequestAttribute Long userId) {
        
        BindInviteResponse response = growthApplicationService
                .bindInviteCode(tenantId, userId, request);
        return Result.success(response);
    }
}
