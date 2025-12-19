package com.bluecone.app.growth.application.facade;

import com.bluecone.app.growth.api.dto.BindInviteRequest;
import com.bluecone.app.growth.api.dto.BindInviteResponse;
import com.bluecone.app.growth.api.dto.InviteCodeResponse;
import com.bluecone.app.growth.api.facade.GrowthInviteFacade;
import com.bluecone.app.growth.application.GrowthApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 增长引擎-邀请门面实现
 */
@Service
@RequiredArgsConstructor
public class GrowthInviteFacadeImpl implements GrowthInviteFacade {
    
    private final GrowthApplicationService growthApplicationService;
    
    @Override
    public InviteCodeResponse getOrCreateInviteCode(Long tenantId, Long userId, String campaignCode) {
        return growthApplicationService.getOrCreateInviteCode(tenantId, userId, campaignCode);
    }
    
    @Override
    public BindInviteResponse bindInviteCode(Long tenantId, Long userId, BindInviteRequest request) {
        return growthApplicationService.bindInviteCode(tenantId, userId, request);
    }
}
