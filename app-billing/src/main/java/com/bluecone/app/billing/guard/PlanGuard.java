package com.bluecone.app.billing.guard;

import com.bluecone.app.billing.application.SubscriptionPlanService;
import com.bluecone.app.billing.dao.entity.TenantSubscriptionDO;
import com.bluecone.app.billing.dao.mapper.TenantSubscriptionMapper;
import com.bluecone.app.billing.domain.enums.SubscriptionStatus;
import com.bluecone.app.billing.domain.error.BillingErrorCode;
import com.bluecone.app.core.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 套餐权限守卫
 * 根据租户订阅状态和套餐等级，限制功能访问
 * 
 * 限制策略：
 * - ACTIVE: 全部功能可用
 * - GRACE: 限制写操作和高成本功能（可配置）
 * - EXPIRED: 仅保留基础查询功能
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlanGuard {
    
    private final TenantSubscriptionMapper subscriptionMapper;
    private final SubscriptionPlanService subscriptionPlanService;
    
    /**
     * 检查租户是否可以执行写操作
     * 宽限期内限制写操作
     */
    public void checkWritePermission(Long tenantId, String operation) {
        TenantSubscriptionDO subscription = getSubscription(tenantId);
        
        if (subscription == null) {
            // 没有订阅，使用免费版，允许基础写操作
            return;
        }
        
        SubscriptionStatus status = SubscriptionStatus.fromCode(subscription.getStatus());
        
        if (status == null) {
            log.warn("[plan-guard] 订阅状态未知，tenantId={}, status={}", tenantId, subscription.getStatus());
            return;
        }
        
        // GRACE 状态下限制写操作
        if (status.isGrace()) {
            log.warn("[plan-guard] 宽限期内限制写操作，tenantId={}, operation={}", tenantId, operation);
            throw new BizException(BillingErrorCode.GRACE_PERIOD_WRITE_RESTRICTED, 
                    "您的订阅已到期，当前处于宽限期。宽限期内限制写操作，请续费以恢复全部功能。");
        }
        
        // EXPIRED 状态下限制写操作
        if (status.isExpired()) {
            log.warn("[plan-guard] 订阅已过期，限制写操作，tenantId={}, operation={}", tenantId, operation);
            throw new BizException(BillingErrorCode.SUBSCRIPTION_EXPIRED_WRITE_RESTRICTED, 
                    "您的订阅已过期，限制写操作。请续费以恢复全部功能。");
        }
    }
    
    /**
     * 检查租户是否可以使用高级功能
     * 宽限期内限制高级功能
     */
    public void checkAdvancedFeature(Long tenantId, String featureName) {
        TenantSubscriptionDO subscription = getSubscription(tenantId);
        
        if (subscription == null) {
            // 没有订阅，使用免费版，不允许高级功能
            throw new BizException(BillingErrorCode.PLAN_FEATURE_RESTRICTED, 
                    String.format("免费版不支持 %s 功能，请升级套餐。", featureName));
        }
        
        SubscriptionStatus status = SubscriptionStatus.fromCode(subscription.getStatus());
        
        if (status == null) {
            log.warn("[plan-guard] 订阅状态未知，tenantId={}, status={}", tenantId, subscription.getStatus());
            return;
        }
        
        // GRACE 状态下限制高级功能
        if (status.isGrace()) {
            log.warn("[plan-guard] 宽限期内限制高级功能，tenantId={}, feature={}", tenantId, featureName);
            throw new BizException(BillingErrorCode.GRACE_PERIOD_FEATURE_RESTRICTED, 
                    String.format("您的订阅已到期，当前处于宽限期。宽限期内限制 %s 功能，请续费以恢复。", featureName));
        }
        
        // EXPIRED 状态下限制高级功能
        if (status.isExpired()) {
            log.warn("[plan-guard] 订阅已过期，限制高级功能，tenantId={}, feature={}", tenantId, featureName);
            throw new BizException(BillingErrorCode.SUBSCRIPTION_EXPIRED_FEATURE_RESTRICTED, 
                    String.format("您的订阅已过期，限制 %s 功能。请续费以恢复。", featureName));
        }
    }
    
    /**
     * 检查租户是否可以创建资源（如门店、用户等）
     * 根据套餐配额限制
     */
    public void checkResourceQuota(Long tenantId, String resourceType, int currentCount) {
        TenantSubscriptionDO subscription = getSubscription(tenantId);
        
        if (subscription == null) {
            // 没有订阅，使用免费版配额
            checkFreeQuota(resourceType, currentCount);
            return;
        }
        
        SubscriptionStatus status = SubscriptionStatus.fromCode(subscription.getStatus());
        
        // GRACE 或 EXPIRED 状态下，限制创建新资源
        if (status != null && (status.isGrace() || status.isExpired())) {
            log.warn("[plan-guard] 订阅状态异常，限制创建资源，tenantId={}, status={}, resourceType={}", 
                    tenantId, status.getCode(), resourceType);
            throw new BizException(BillingErrorCode.SUBSCRIPTION_STATUS_RESTRICTED, 
                    "您的订阅状态异常，限制创建新资源。请续费以恢复。");
        }
        
        // TODO: 根据套餐配额检查资源数量限制
        // 可以从 subscription.getCurrentFeatures() 中读取配额
    }
    
    /**
     * 检查是否在宽限期
     */
    public boolean isInGracePeriod(Long tenantId) {
        TenantSubscriptionDO subscription = getSubscription(tenantId);
        if (subscription == null) {
            return false;
        }
        
        SubscriptionStatus status = SubscriptionStatus.fromCode(subscription.getStatus());
        return status != null && status.isGrace();
    }
    
    /**
     * 检查订阅是否有效（ACTIVE 或 GRACE）
     */
    public boolean isSubscriptionValid(Long tenantId) {
        TenantSubscriptionDO subscription = getSubscription(tenantId);
        if (subscription == null) {
            return false;
        }
        
        SubscriptionStatus status = SubscriptionStatus.fromCode(subscription.getStatus());
        return status != null && status.canUseBasicFeatures();
    }
    
    private TenantSubscriptionDO getSubscription(Long tenantId) {
        return subscriptionMapper.selectOne(
            new LambdaQueryWrapper<TenantSubscriptionDO>()
                .eq(TenantSubscriptionDO::getTenantId, tenantId)
        );
    }
    
    private void checkFreeQuota(String resourceType, int currentCount) {
        // 免费版配额限制
        int maxCount = switch (resourceType) {
            case "store" -> 1;
            case "user" -> 2;
            default -> 10;
        };
        
        if (currentCount >= maxCount) {
            throw new BizException(BillingErrorCode.FREE_PLAN_QUOTA_EXCEEDED, 
                    String.format("免费版 %s 数量已达上限（%d），请升级套餐。", resourceType, maxCount));
        }
    }
}
