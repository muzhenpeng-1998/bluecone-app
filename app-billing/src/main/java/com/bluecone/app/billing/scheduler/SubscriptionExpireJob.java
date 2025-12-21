package com.bluecone.app.billing.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.billing.dao.entity.TenantSubscriptionDO;
import com.bluecone.app.billing.dao.mapper.TenantSubscriptionMapper;
import com.bluecone.app.billing.domain.enums.SubscriptionStatus;
import com.bluecone.app.billing.domain.service.BillingDomainService;
import com.bluecone.app.billing.metrics.BillingMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订阅到期任务
 * 定时扫描已到期的订阅，按照宽限期策略处理：
 * ACTIVE -> GRACE（到期进入宽限期，7天）
 * GRACE -> EXPIRED（宽限期结束，降级到免费版）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionExpireJob {
    
    private final TenantSubscriptionMapper subscriptionMapper;
    private final BillingDomainService billingDomainService;
    private final BillingMetrics billingMetrics;
    
    // 宽限期天数
    private static final int GRACE_PERIOD_DAYS = 7;
    
    /**
     * 每小时执行一次，扫描已到期的订阅
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void expireSubscriptions() {
//        log.info("[subscription-expire-job] 开始扫描已到期的订阅");
        
        try {
            // 处理 ACTIVE -> GRACE
            handleActiveToGrace();
            
            // 处理 GRACE -> EXPIRED
            handleGraceToExpired();
            
//            log.info("[subscription-expire-job] 订阅到期处理完成");
            
        } catch (Exception e) {
            log.error("[subscription-expire-job] 订阅到期任务执行失败", e);
        }
    }
    
    /**
     * 处理 ACTIVE -> GRACE
     * 扫描已到期但状态仍为 ACTIVE 的订阅，将其转为 GRACE 状态
     */
    private void handleActiveToGrace() {
        LocalDateTime now = LocalDateTime.now();
        
        // 查询已到期但状态仍为 ACTIVE 的订阅
        List<TenantSubscriptionDO> expiredSubscriptions = subscriptionMapper.selectList(
            new LambdaQueryWrapper<TenantSubscriptionDO>()
                .eq(TenantSubscriptionDO::getStatus, SubscriptionStatus.ACTIVE.getCode())
                .lt(TenantSubscriptionDO::getSubscriptionEndAt, now)
        );
        
        if (expiredSubscriptions.isEmpty()) {
//            log.info("[subscription-expire-job] 没有需要进入宽限期的订阅");
            return;
        }
        
//        log.info("[subscription-expire-job] 发现 {} 个需要进入宽限期的订阅", expiredSubscriptions.size());
        
        int successCount = 0;
        int failCount = 0;
        
        for (TenantSubscriptionDO subscription : expiredSubscriptions) {
            try {
                // 进入宽限期
                subscription.setStatus(SubscriptionStatus.GRACE.getCode());
                subscription.setUpdatedAt(now);
                subscriptionMapper.updateById(subscription);
                
                // 记录指标
                billingMetrics.recordGraceEntered(subscription.getTenantId(), subscription.getCurrentPlanCode());
                
                successCount++;
                
                log.info("[subscription-expire-job] 订阅进入宽限期，tenantId={}, planCode={}, endAt={}, graceEndAt={}", 
                        subscription.getTenantId(), 
                        subscription.getCurrentPlanCode(), 
                        subscription.getSubscriptionEndAt(),
                        subscription.getSubscriptionEndAt().plusDays(GRACE_PERIOD_DAYS));
            } catch (Exception e) {
                failCount++;
                log.error("[subscription-expire-job] 订阅进入宽限期失败，tenantId={}, subscriptionId={}", 
                        subscription.getTenantId(), subscription.getId(), e);
            }
        }
        
        log.info("[subscription-expire-job] ACTIVE->GRACE 处理完成，总数={}, 成功={}, 失败={}", 
                expiredSubscriptions.size(), successCount, failCount);
    }
    
    /**
     * 处理 GRACE -> EXPIRED
     * 扫描宽限期已结束的订阅，降级到免费版
     */
    private void handleGraceToExpired() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime graceExpireTime = now.minusDays(GRACE_PERIOD_DAYS);
        
        // 查询宽限期已结束的订阅（到期时间 + 7天 < 当前时间）
        List<TenantSubscriptionDO> graceExpiredSubscriptions = subscriptionMapper.selectList(
            new LambdaQueryWrapper<TenantSubscriptionDO>()
                .eq(TenantSubscriptionDO::getStatus, SubscriptionStatus.GRACE.getCode())
                .lt(TenantSubscriptionDO::getSubscriptionEndAt, graceExpireTime)
        );
        
        if (graceExpiredSubscriptions.isEmpty()) {
//            log.info("[subscription-expire-job] 没有宽限期已结束的订阅");
            return;
        }
        
        log.info("[subscription-expire-job] 发现 {} 个宽限期已结束的订阅", graceExpiredSubscriptions.size());
        
        int successCount = 0;
        int failCount = 0;
        
        for (TenantSubscriptionDO subscription : graceExpiredSubscriptions) {
            try {
                // 降级到免费版
                String oldPlanCode = subscription.getCurrentPlanCode();
                billingDomainService.downgradeToFree(subscription.getTenantId());
                
                // 记录指标
                billingMetrics.recordGraceExpired(subscription.getTenantId(), oldPlanCode);
                
                successCount++;
                
                log.info("[subscription-expire-job] 订阅降级到免费版，tenantId={}, oldPlanCode={}, endAt={}", 
                        subscription.getTenantId(), oldPlanCode, subscription.getSubscriptionEndAt());
            } catch (Exception e) {
                failCount++;
                log.error("[subscription-expire-job] 订阅降级失败，tenantId={}, subscriptionId={}", 
                        subscription.getTenantId(), subscription.getId(), e);
            }
        }
        
        log.info("[subscription-expire-job] GRACE->EXPIRED 处理完成，总数={}, 成功={}, 失败={}", 
                graceExpiredSubscriptions.size(), successCount, failCount);
    }
}
