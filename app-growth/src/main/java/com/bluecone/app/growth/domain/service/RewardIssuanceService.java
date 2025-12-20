package com.bluecone.app.growth.domain.service;

import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.growth.api.dto.RewardConfig;
import com.bluecone.app.growth.api.enums.IssueStatus;
import com.bluecone.app.growth.api.enums.RewardType;
import com.bluecone.app.growth.api.enums.UserRole;
import com.bluecone.app.growth.domain.model.RewardIssueLog;
import com.bluecone.app.growth.domain.repository.RewardIssueLogRepository;
import com.bluecone.app.id.api.IdScope;
import com.bluecone.app.id.api.IdService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 奖励发放服务
 * 负责奖励发放的幂等控制和结果记录
 */
@Slf4j
@Service
public class RewardIssuanceService {
    
    private final RewardIssueLogRepository rewardIssueLogRepository;
    private final IdService idService;
    private final ObjectMapper objectMapper;
    private final GrowthMetrics metrics;

    public RewardIssuanceService(RewardIssueLogRepository rewardIssueLogRepository,
                                IdService idService,
                                @Qualifier("redisObjectMapper") ObjectMapper objectMapper,
                                GrowthMetrics metrics) {
        this.rewardIssueLogRepository = rewardIssueLogRepository;
        this.idService = idService;
        this.objectMapper = objectMapper;
        this.metrics = metrics;
    }
    
    // 具体的奖励发放器将在后续注入
    private CouponRewardIssuer couponRewardIssuer;
    private WalletRewardIssuer walletRewardIssuer;
    private PointsRewardIssuer pointsRewardIssuer;
    
    public void setCouponRewardIssuer(CouponRewardIssuer issuer) {
        this.couponRewardIssuer = issuer;
    }
    
    public void setWalletRewardIssuer(WalletRewardIssuer issuer) {
        this.walletRewardIssuer = issuer;
    }
    
    public void setPointsRewardIssuer(PointsRewardIssuer issuer) {
        this.pointsRewardIssuer = issuer;
    }
    
    /**
     * 发放奖励（幂等）
     * 
     * @param tenantId 租户ID
     * @param campaignCode 活动编码
     * @param attributionId 归因ID
     * @param userId 用户ID
     * @param userRole 用户角色
     * @param rewardConfig 奖励配置
     * @param triggerOrderId 触发订单ID
     * @return 发放日志
     */
    @Transactional
    public RewardIssueLog issueReward(Long tenantId,
                                     String campaignCode,
                                     Long attributionId,
                                     Long userId,
                                     UserRole userRole,
                                     RewardConfig rewardConfig,
                                     Long triggerOrderId) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 构造幂等键：tenant:campaign:attribution:userId:role:rewardType
            String idempotencyKey = buildIdempotencyKey(tenantId, campaignCode, 
                    attributionId, userId, userRole, rewardConfig.getType());
            
            // 2. 幂等检查
            Optional<RewardIssueLog> existingLog = rewardIssueLogRepository
                    .findByIdempotencyKey(tenantId, idempotencyKey);
            
            if (existingLog.isPresent()) {
                RewardIssueLog issueLog = existingLog.get();
                if (issueLog.isSuccess()) {
                    log.info("奖励已发放（幂等返回）: idempotencyKey={}, resultId={}", 
                            idempotencyKey, issueLog.getResultId());
                    return issueLog;
                } else if (issueLog.isFailed()) {
                    throw new BusinessException(issueLog.getErrorCode(), issueLog.getErrorMessage());
                } else {
                    throw new BusinessException("REWARD_ISSUE_IN_PROGRESS", "奖励发放处理中");
                }
            }
            
            // 3. 创建发放日志（PROCESSING状态）
            RewardIssueLog issueLog = createProcessingLog(tenantId, campaignCode, attributionId, 
                    userId, userRole, rewardConfig, triggerOrderId, idempotencyKey);
            
            try {
                rewardIssueLogRepository.save(issueLog);
            } catch (DuplicateKeyException e) {
                // 并发情况下，唯一约束冲突
                log.warn("奖励发放幂等键冲突: idempotencyKey={}", idempotencyKey);
                throw new BusinessException("DUPLICATE_REWARD_ISSUE", "重复的奖励发放请求");
            }
            
            // 4. 执行实际发放
            String resultId;
            try {
                resultId = doIssueReward(tenantId, userId, rewardConfig, idempotencyKey);
                
                // 5. 更新日志为成功
                issueLog.markSuccess(resultId);
                rewardIssueLogRepository.update(issueLog);
                
                metrics.recordRewardIssued(rewardConfig.getType().name());
                log.info("奖励发放成功: userId={}, rewardType={}, resultId={}", 
                        userId, rewardConfig.getType(), resultId);
                
                return issueLog;
                
            } catch (BusinessException e) {
                // 业务异常
                metrics.recordRewardFailed(e.getCode());
                issueLog.markFailed(e.getCode(), e.getMessage());
                rewardIssueLogRepository.update(issueLog);
                throw e;
            } catch (Exception e) {
                // 系统异常
                metrics.recordRewardFailed("SYSTEM_ERROR");
                issueLog.markFailed("SYSTEM_ERROR", "系统错误: " + e.getMessage());
                rewardIssueLogRepository.update(issueLog);
                log.error("奖励发放失败: userId={}, rewardType={}", userId, rewardConfig.getType(), e);
                throw new BusinessException("REWARD_ISSUE_FAILED", "奖励发放失败: " + e.getMessage());
            }
            
        } finally {
            metrics.recordRewardDuration(startTime);
        }
    }
    
    /**
     * 执行实际发放（调用相应的奖励发放器）
     */
    private String doIssueReward(Long tenantId, Long userId, RewardConfig config, String idempotencyKey) {
        return switch (config.getType()) {
            case COUPON -> {
                if (couponRewardIssuer == null) {
                    throw new BusinessException("COUPON_ISSUER_NOT_AVAILABLE", "优惠券发放器未配置");
                }
                yield couponRewardIssuer.issue(tenantId, userId, config.getValue(), idempotencyKey);
            }
            case WALLET_CREDIT -> {
                if (walletRewardIssuer == null) {
                    throw new BusinessException("WALLET_ISSUER_NOT_AVAILABLE", "储值发放器未配置");
                }
                yield walletRewardIssuer.issue(tenantId, userId, config.getValue(), idempotencyKey);
            }
            case POINTS -> {
                if (pointsRewardIssuer == null) {
                    throw new BusinessException("POINTS_ISSUER_NOT_AVAILABLE", "积分发放器未配置");
                }
                yield pointsRewardIssuer.issue(tenantId, userId, config.getValue(), idempotencyKey);
            }
        };
    }
    
    /**
     * 创建处理中的发放日志
     */
    private RewardIssueLog createProcessingLog(Long tenantId, String campaignCode,
                                              Long attributionId, Long userId, UserRole userRole,
                                              RewardConfig config, Long triggerOrderId,
                                              String idempotencyKey) {
        Long logId = idService.nextLong(IdScope.GROWTH);
        
        return RewardIssueLog.builder()
                .id(logId)
                .tenantId(tenantId)
                .campaignCode(campaignCode)
                .idempotencyKey(idempotencyKey)
                .attributionId(attributionId)
                .userId(userId)
                .userRole(userRole)
                .rewardType(config.getType())
                .rewardValue(config.getValue())
                .issueStatus(IssueStatus.PROCESSING)
                .triggerOrderId(triggerOrderId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * 构建幂等键
     */
    private String buildIdempotencyKey(Long tenantId, String campaignCode, Long attributionId,
                                      Long userId, UserRole userRole, RewardType rewardType) {
        return String.format("reward:%d:%s:%d:%d:%s:%s",
                tenantId, campaignCode, attributionId, userId, userRole.name(), rewardType.name());
    }
    
    /**
     * 优惠券奖励发放器接口
     */
    public interface CouponRewardIssuer {
        /**
         * 发放优惠券
         * @param tenantId 租户ID
         * @param userId 用户ID
         * @param templateIdStr 模板ID（JSON格式）
         * @param idempotencyKey 幂等键
         * @return 券ID
         */
        String issue(Long tenantId, Long userId, String templateIdStr, String idempotencyKey);
    }
    
    /**
     * 储值奖励发放器接口
     */
    public interface WalletRewardIssuer {
        /**
         * 发放储值
         * @param tenantId 租户ID
         * @param userId 用户ID
         * @param amountStr 金额（JSON格式，单位：分）
         * @param idempotencyKey 幂等键
         * @return 账本流水ID
         */
        String issue(Long tenantId, Long userId, String amountStr, String idempotencyKey);
    }
    
    /**
     * 积分奖励发放器接口
     */
    public interface PointsRewardIssuer {
        /**
         * 发放积分
         * @param tenantId 租户ID
         * @param userId 用户ID
         * @param pointsStr 积分值（JSON格式）
         * @param idempotencyKey 幂等键
         * @return 积分流水ID
         */
        String issue(Long tenantId, Long userId, String pointsStr, String idempotencyKey);
    }
}
