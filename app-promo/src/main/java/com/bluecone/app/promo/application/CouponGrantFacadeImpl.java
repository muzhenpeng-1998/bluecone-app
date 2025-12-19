package com.bluecone.app.promo.application;

import com.bluecone.app.promo.api.dto.CouponGrantCommand;
import com.bluecone.app.promo.api.dto.CouponGrantResult;
import com.bluecone.app.promo.api.enums.GrantSource;
import com.bluecone.app.promo.api.facade.CouponGrantFacade;
import com.bluecone.app.promo.domain.model.Coupon;
import com.bluecone.app.promo.domain.service.CouponGrantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 优惠券发放门面实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponGrantFacadeImpl implements CouponGrantFacade {

    private final CouponGrantService couponGrantService;

    @Override
    public CouponGrantResult grantCoupon(CouponGrantCommand command) {
        try {
            // 确定发放来源
            GrantSource grantSource = determineGrantSource(command.getSource());
            
            // 调用领域服务发放券
            Coupon coupon = couponGrantService.grantCoupon(
                    command.getTenantId(),
                    command.getTemplateId(),
                    command.getUserId(),
                    command.getIdempotencyKey(),
                    grantSource,
                    null,  // operatorId - 系统发放时为null
                    null,  // operatorName - 系统发放时为null
                    command.getGrantReason()
            );
            
            return CouponGrantResult.builder()
                    .success(true)
                    .couponId(coupon.getId())
                    .build();
            
        } catch (Exception e) {
            log.error("[coupon-grant-facade] 发券失败, command={}", command, e);
            return CouponGrantResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }
    
    /**
     * 确定发放来源
     */
    private GrantSource determineGrantSource(String source) {
        if (source == null || source.isBlank()) {
            return GrantSource.CAMPAIGN;
        }
        
        try {
            return GrantSource.valueOf(source.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("[coupon-grant-facade] 未知的发放来源: {}, 使用默认值 CAMPAIGN", source);
            return GrantSource.CAMPAIGN;
        }
    }
}
