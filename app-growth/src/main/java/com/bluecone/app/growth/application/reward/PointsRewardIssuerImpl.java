package com.bluecone.app.growth.application.reward;

import com.bluecone.app.growth.domain.service.RewardIssuanceService.PointsRewardIssuer;
import com.bluecone.app.id.api.IdScope;
import com.bluecone.app.id.api.IdService;
import com.bluecone.app.member.domain.model.PointsLedger;
import com.bluecone.app.member.domain.service.PointsDomainService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 积分奖励发放器实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointsRewardIssuerImpl implements PointsRewardIssuer {
    
    private final PointsDomainService pointsDomainService;
    private final IdService idService;
    private final ObjectMapper objectMapper;
    
    @Override
    public String issue(Long tenantId, Long userId, String pointsStr, String idempotencyKey) {
        try {
            // 解析积分值
            Map<String, Object> valueMap = objectMapper.readValue(pointsStr, Map.class);
            Long points = Long.valueOf(valueMap.get("points").toString());
            
            // 调用积分服务
            Long ledgerId = idService.nextLong(IdScope.POINTS_LEDGER);
            PointsLedger ledger = pointsDomainService.earnPoints(
                    ledgerId,
                    tenantId,
                    userId,
                    points,
                    "GROWTH_REWARD",
                    idempotencyKey,
                    idempotencyKey,
                    "邀新活动奖励"
            );
            
            return String.valueOf(ledger.getId());
            
        } catch (Exception e) {
            log.error("积分奖励发放失败: tenantId={}, userId={}, pointsStr={}", 
                    tenantId, userId, pointsStr, e);
            throw new RuntimeException("积分发放失败: " + e.getMessage(), e);
        }
    }
}
