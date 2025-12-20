package com.bluecone.app.growth.application.reward;

import com.bluecone.app.growth.domain.service.RewardIssuanceService.CouponRewardIssuer;
import com.bluecone.app.promo.api.enums.GrantSource;
import com.bluecone.app.promo.domain.model.Coupon;
import com.bluecone.app.promo.domain.service.CouponGrantService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 优惠券奖励发放器实现
 */
@Slf4j
@Component
public class CouponRewardIssuerImpl implements CouponRewardIssuer {
    
    private final CouponGrantService couponGrantService;
    private final ObjectMapper objectMapper;

    public CouponRewardIssuerImpl(CouponGrantService couponGrantService,
                                 @Qualifier("redisObjectMapper") ObjectMapper objectMapper) {
        this.couponGrantService = couponGrantService;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public String issue(Long tenantId, Long userId, String templateIdStr, String idempotencyKey) {
        try {
            // 解析模板ID
            Map<String, Object> valueMap = objectMapper.readValue(templateIdStr, Map.class);
            Long templateId = Long.valueOf(valueMap.get("templateId").toString());
            
            // 调用优惠券发放服务
            Coupon coupon = couponGrantService.grantCoupon(
                    tenantId, 
                    templateId, 
                    userId, 
                    idempotencyKey,
                    GrantSource.CAMPAIGN,
                    null, 
                    "增长活动",
                    "邀新活动奖励"
            );
            
            return String.valueOf(coupon.getId());
            
        } catch (Exception e) {
            log.error("优惠券奖励发放失败: tenantId={}, userId={}, templateIdStr={}", 
                    tenantId, userId, templateIdStr, e);
            throw new RuntimeException("优惠券发放失败: " + e.getMessage(), e);
        }
    }
}
