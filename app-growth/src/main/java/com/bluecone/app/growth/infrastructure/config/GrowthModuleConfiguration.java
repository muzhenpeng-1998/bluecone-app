package com.bluecone.app.growth.infrastructure.config;

import com.bluecone.app.growth.application.reward.CouponRewardIssuerImpl;
import com.bluecone.app.growth.application.reward.PointsRewardIssuerImpl;
import com.bluecone.app.growth.application.reward.WalletRewardIssuerImpl;
import com.bluecone.app.growth.domain.service.RewardIssuanceService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 增长模块配置
 */
@Slf4j
@Configuration
@ComponentScan(basePackages = "com.bluecone.app.growth")
@RequiredArgsConstructor
public class GrowthModuleConfiguration {
    
    private final RewardIssuanceService rewardIssuanceService;
    private final CouponRewardIssuerImpl couponRewardIssuer;
    private final WalletRewardIssuerImpl walletRewardIssuer;
    private final PointsRewardIssuerImpl pointsRewardIssuer;
    
    @PostConstruct
    public void init() {
        // 注入奖励发放器
        rewardIssuanceService.setCouponRewardIssuer(couponRewardIssuer);
        rewardIssuanceService.setWalletRewardIssuer(walletRewardIssuer);
        rewardIssuanceService.setPointsRewardIssuer(pointsRewardIssuer);
        
        log.info("增长引擎模块初始化完成");
    }
}
