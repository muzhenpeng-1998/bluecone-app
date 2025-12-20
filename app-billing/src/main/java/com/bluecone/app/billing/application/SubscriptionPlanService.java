package com.bluecone.app.billing.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.billing.dao.entity.PlanSkuDO;
import com.bluecone.app.billing.dao.entity.TenantSubscriptionDO;
import com.bluecone.app.billing.dao.mapper.PlanSkuMapper;
import com.bluecone.app.billing.dao.mapper.TenantSubscriptionMapper;
import com.bluecone.app.billing.domain.enums.PlanCode;
import com.bluecone.app.billing.domain.enums.SubscriptionStatus;
import com.bluecone.app.core.config.Feature;
import com.bluecone.app.core.config.domain.TenantPlanConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 订阅套餐服务
 * 提供租户套餐配额查询能力，供 PlanGuard 使用
 */
@Slf4j
@Service
public class SubscriptionPlanService {
    
    private final TenantSubscriptionMapper subscriptionMapper;
    private final PlanSkuMapper planSkuMapper;
    private final ObjectMapper objectMapper;

    public SubscriptionPlanService(TenantSubscriptionMapper subscriptionMapper,
                                  PlanSkuMapper planSkuMapper,
                                  @Qualifier("redisObjectMapper") ObjectMapper objectMapper) {
        this.subscriptionMapper = subscriptionMapper;
        this.planSkuMapper = planSkuMapper;
        this.objectMapper = objectMapper;
    }
    
    /**
     * 获取租户的套餐配置
     */
    public TenantPlanConfig getTenantPlanConfig(Long tenantId) {
        // 查询租户订阅
        TenantSubscriptionDO subscription = subscriptionMapper.selectOne(
            new LambdaQueryWrapper<TenantSubscriptionDO>()
                .eq(TenantSubscriptionDO::getTenantId, tenantId)
        );
        
        // 如果没有订阅，返回免费版配置
        if (subscription == null) {
            log.info("[subscription-plan] 租户无订阅记录，返回免费版配置，tenantId={}", tenantId);
            return getFreePlanConfig();
        }
        
        // 如果订阅已过期，返回免费版配置
        if (SubscriptionStatus.EXPIRED.getCode().equals(subscription.getStatus()) ||
            subscription.getSubscriptionEndAt().isBefore(LocalDateTime.now())) {
            log.info("[subscription-plan] 租户订阅已过期，返回免费版配置，tenantId={}, endAt={}", 
                    tenantId, subscription.getSubscriptionEndAt());
            return getFreePlanConfig();
        }
        
        // 解析订阅配置
        Map<String, Object> features = parseFeatures(subscription.getCurrentFeatures());
        
        return new SubscriptionBasedPlanConfig(
                subscription.getCurrentPlanCode(),
                subscription.getCurrentPlanLevel(),
                features
        );
    }
    
    /**
     * 获取免费版配置
     */
    private TenantPlanConfig getFreePlanConfig() {
        PlanSkuDO freePlan = planSkuMapper.selectOne(
            new LambdaQueryWrapper<PlanSkuDO>()
                .eq(PlanSkuDO::getPlanCode, PlanCode.FREE.getCode())
                .eq(PlanSkuDO::getStatus, "ACTIVE")
                .last("LIMIT 1")
        );
        
        if (freePlan == null) {
            log.error("[subscription-plan] 免费版套餐不存在，返回默认配置");
            return getDefaultFreePlanConfig();
        }
        
        Map<String, Object> features = parseFeatures(freePlan.getFeatures());
        
        return new SubscriptionBasedPlanConfig(
                freePlan.getPlanCode(),
                freePlan.getPlanLevel(),
                features
        );
    }
    
    /**
     * 获取默认免费版配置（兜底）
     */
    private TenantPlanConfig getDefaultFreePlanConfig() {
        Map<String, Object> features = new HashMap<>();
        features.put("maxStores", 1);
        features.put("maxUsers", 2);
        features.put("hasMultiWarehouse", false);
        features.put("hasAdvancedReports", false);
        features.put("hasPrioritySupport", false);
        
        return new SubscriptionBasedPlanConfig(
                PlanCode.FREE.getCode(),
                PlanCode.FREE.getLevel(),
                features
        );
    }
    
    private Map<String, Object> parseFeatures(String featuresJson) {
        if (featuresJson == null || featuresJson.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(featuresJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("[subscription-plan] 解析 features JSON 失败，featuresJson={}", featuresJson, e);
            return new HashMap<>();
        }
    }
    
    /**
     * 基于订阅的套餐配置实现
     */
    private static class SubscriptionBasedPlanConfig implements TenantPlanConfig {
        
        private final String planCode;
        private final int planLevel;
        private final Map<String, Object> features;
        
        public SubscriptionBasedPlanConfig(String planCode, int planLevel, Map<String, Object> features) {
            this.planCode = planCode;
            this.planLevel = planLevel;
            this.features = features;
        }
        
        @Override
        public String planLevel() {
            return planCode;
        }
        
        @Override
        public boolean hasFeature(Feature feature) {
            // 根据 Feature 枚举查询配额
            String featureKey = feature.name();
            Object value = features.get(featureKey);
            
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
            
            // 默认返回 false
            return false;
        }
        
        @Override
        public int maxStores() {
            return getIntValue("maxStores", 1);
        }
        
        @Override
        public int maxUsers() {
            return getIntValue("maxUsers", 2);
        }
        
        private int getIntValue(String key, int defaultValue) {
            Object value = features.get(key);
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            try {
                return Integer.parseInt(value.toString());
            } catch (Exception e) {
                return defaultValue;
            }
        }
    }
}
