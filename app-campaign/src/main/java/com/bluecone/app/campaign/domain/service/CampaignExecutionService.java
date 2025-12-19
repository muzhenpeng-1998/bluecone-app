package com.bluecone.app.campaign.domain.service;

import com.bluecone.app.campaign.api.dto.CampaignRulesDTO;
import com.bluecone.app.campaign.api.enums.CampaignType;
import com.bluecone.app.campaign.domain.model.Campaign;
import com.bluecone.app.campaign.domain.model.ExecutionLog;
import com.bluecone.app.campaign.domain.repository.ExecutionLogRepository;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.id.api.IdScope;
import com.bluecone.app.id.api.IdService;
import com.bluecone.app.promo.api.dto.CouponGrantCommand;
import com.bluecone.app.promo.api.dto.CouponGrantResult;
import com.bluecone.app.promo.api.facade.CouponGrantFacade;
import com.bluecone.app.wallet.api.dto.WalletAssetCommand;
import com.bluecone.app.wallet.api.dto.WalletAssetResult;
import com.bluecone.app.wallet.api.facade.WalletAssetFacade;
import com.bluecone.app.notify.api.dto.EnqueueNotificationRequest;
import com.bluecone.app.notify.api.enums.NotificationChannel;
import com.bluecone.app.notify.api.facade.NotificationFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 活动执行服务
 * 负责异步活动的执行（返券、充值赠送）
 */
@Slf4j
@Service
public class CampaignExecutionService {
    
    private final ExecutionLogRepository executionLogRepository;
    private final IdService idService;
    private final CouponGrantFacade couponGrantFacade;
    private final WalletAssetFacade walletAssetFacade;
    private final CampaignMetrics campaignMetrics;
    
    @Autowired(required = false)
    private NotificationFacade notificationFacade;
    
    public CampaignExecutionService(
            ExecutionLogRepository executionLogRepository,
            IdService idService,
            CouponGrantFacade couponGrantFacade,
            WalletAssetFacade walletAssetFacade,
            CampaignMetrics campaignMetrics) {
        this.executionLogRepository = executionLogRepository;
        this.idService = idService;
        this.couponGrantFacade = couponGrantFacade;
        this.walletAssetFacade = walletAssetFacade;
        this.campaignMetrics = campaignMetrics;
    }
    
    /**
     * 执行活动（幂等）
     * 
     * @param campaign 活动
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param bizOrderId 业务单ID
     * @param bizOrderNo 业务单号
     * @param bizAmount 业务金额
     * @return 执行结果
     */
    @Transactional(rollbackFor = Exception.class)
    public ExecutionLog executeCampaign(Campaign campaign, 
                                        Long tenantId, 
                                        Long userId, 
                                        Long bizOrderId,
                                        String bizOrderNo,
                                        BigDecimal bizAmount) {
        
        String idempotencyKey = buildIdempotencyKey(tenantId, campaign.getCampaignType(), bizOrderId, userId);
        
        // 1. 幂等检查
        Optional<ExecutionLog> existingLog = executionLogRepository.findByIdempotencyKey(tenantId, idempotencyKey);
        if (existingLog.isPresent()) {
            log.info("[campaign-exec] 幂等返回，idempotencyKey={}", idempotencyKey);
            return existingLog.get();
        }
        
        // 2. 创建执行日志
        ExecutionLog execLog = ExecutionLog.builder()
                .id(idService.nextLong(IdScope.CAMPAIGN_EXECUTION_LOG))
                .tenantId(tenantId)
                .campaignId(campaign.getId())
                .campaignCode(campaign.getCampaignCode())
                .campaignType(campaign.getCampaignType())
                .idempotencyKey(idempotencyKey)
                .userId(userId)
                .bizOrderId(bizOrderId)
                .bizOrderNo(bizOrderNo)
                .bizAmount(bizAmount)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        try {
            // 3. 执行活动逻辑
            executeByType(campaign, execLog, tenantId, userId, bizOrderId, bizAmount);
            
            // 4. 保存执行日志（成功）
            executionLogRepository.save(execLog);
            
            // 5. 记录指标
            campaignMetrics.recordExecutionSuccess(
                    campaign.getCampaignType().name(), 
                    campaign.getCampaignCode()
            );
            
            // 6. 发送通知
            sendSuccessNotification(campaign, tenantId, userId, bizOrderId, execLog);
            
            log.info("[campaign-exec] 活动执行成功，campaign={}, user={}, order={}, reward={}", 
                    campaign.getCampaignCode(), userId, bizOrderId, execLog.getRewardAmount());
            
            return execLog;
            
        } catch (DuplicateKeyException e) {
            // 并发情况下可能出现唯一键冲突，此时查询并返回已有记录
            log.warn("[campaign-exec] 并发冲突，idempotencyKey={}", idempotencyKey);
            return executionLogRepository.findByIdempotencyKey(tenantId, idempotencyKey)
                    .orElseThrow(() -> new BusinessException("CAMPAIGN_EXEC_ERROR", "活动执行失败"));
            
        } catch (Exception e) {
            log.error("[campaign-exec] 活动执行失败，campaign={}, user={}, order={}", 
                    campaign.getCampaignCode(), userId, bizOrderId, e);
            
            // 记录失败指标
            campaignMetrics.recordExecutionFailure(
                    campaign.getCampaignType().name(),
                    campaign.getCampaignCode(),
                    e.getClass().getSimpleName()
            );
            
            // 保存失败日志
            execLog.markFailed(e.getMessage());
            try {
                executionLogRepository.save(execLog);
            } catch (DuplicateKeyException ex) {
                // 并发情况下可能唯一键冲突，忽略
                log.warn("[campaign-exec] 保存失败日志时并发冲突，idempotencyKey={}", idempotencyKey);
            }
            
            throw new BusinessException("CAMPAIGN_EXEC_ERROR", "活动执行失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据活动类型执行具体逻辑
     */
    private void executeByType(Campaign campaign, ExecutionLog execLog, Long tenantId, Long userId, 
                               Long bizOrderId, BigDecimal bizAmount) {
        
        CampaignType type = campaign.getCampaignType();
        CampaignRulesDTO rules = campaign.getRules();
        
        if (type == CampaignType.ORDER_REBATE_COUPON) {
            // 订单完成返券
            executeOrderRebateCoupon(campaign, execLog, tenantId, userId, bizOrderId, rules);
            
        } else if (type == CampaignType.RECHARGE_BONUS) {
            // 充值赠送
            executeRechargeBonus(campaign, execLog, tenantId, userId, bizOrderId, bizAmount, rules);
            
        } else {
            throw new BusinessException("CAMPAIGN_UNSUPPORTED_TYPE", "不支持的活动类型: " + type);
        }
    }
    
    /**
     * 执行订单返券
     */
    private void executeOrderRebateCoupon(Campaign campaign, ExecutionLog execLog, Long tenantId, 
                                          Long userId, Long orderId, CampaignRulesDTO rules) {
        
        if (rules.getCouponTemplateIds() == null || rules.getCouponTemplateIds().isBlank()) {
            throw new BusinessException("CAMPAIGN_INVALID_RULES", "活动规则缺少券模板ID");
        }
        
        // 解析券模板ID列表
        List<Long> templateIds = Arrays.stream(rules.getCouponTemplateIds().split(","))
                .map(String::trim)
                .map(Long::valueOf)
                .collect(Collectors.toList());
        
        int quantity = rules.getCouponQuantity() != null ? rules.getCouponQuantity() : 1;
        
        List<String> couponIds = new ArrayList<>();
        
        // 为每个模板发放券
        for (Long templateId : templateIds) {
            for (int i = 0; i < quantity; i++) {
                String couponIdempotencyKey = String.format("campaign:%d:order:%d:template:%d:seq:%d", 
                        campaign.getId(), orderId, templateId, i);
                
                CouponGrantCommand command = CouponGrantCommand.builder()
                        .tenantId(tenantId)
                        .templateId(templateId)
                        .userId(userId)
                        .idempotencyKey(couponIdempotencyKey)
                        .grantReason("活动奖励：" + campaign.getCampaignName())
                        .build();
                
                CouponGrantResult result = couponGrantFacade.grantCoupon(command);
                if (result != null && result.getCouponId() != null) {
                    couponIds.add(result.getCouponId().toString());
                }
            }
        }
        
        if (couponIds.isEmpty()) {
            throw new BusinessException("CAMPAIGN_GRANT_FAILED", "券发放失败");
        }
        
        execLog.markSuccess(BigDecimal.ZERO, String.join(",", couponIds));
    }
    
    /**
     * 执行充值赠送
     */
    private void executeRechargeBonus(Campaign campaign, ExecutionLog execLog, Long tenantId, 
                                      Long userId, Long rechargeId, BigDecimal rechargeAmount, 
                                      CampaignRulesDTO rules) {
        
        // 计算赠送金额
        BigDecimal bonusAmount = calculateBonusAmount(rechargeAmount, rules);
        
        if (bonusAmount.compareTo(BigDecimal.ZERO) <= 0) {
            execLog.markSkipped("赠送金额为0");
            campaignMetrics.recordExecutionSkipped(
                    campaign.getCampaignType().name(),
                    campaign.getCampaignCode(),
                    "ZeroAmount"
            );
            return;
        }
        
        // 调用钱包赠送接口
        String bonusIdempotencyKey = String.format("campaign:%d:recharge:%d:bonus", 
                campaign.getId(), rechargeId);
        
        WalletAssetCommand command = WalletAssetCommand.builder()
                .tenantId(tenantId)
                .userId(userId)
                .amount(bonusAmount)
                .bizType("CAMPAIGN_BONUS")
                .bizOrderId(campaign.getId())
                .bizOrderNo(campaign.getCampaignCode())
                .idempotencyKey(bonusIdempotencyKey)
                .remark("活动赠送：" + campaign.getCampaignName())
                .operatorId(0L) // 系统操作
                .build();
        
        WalletAssetResult result = walletAssetFacade.credit(command);
        
        if (!result.isSuccess()) {
            throw new BusinessException("WALLET_CREDIT_FAILED", "钱包赠送失败: " + result.getErrorMessage());
        }
        
        log.info("[campaign-exec] 充值赠送成功，rechargeId={}, bonusAmount={}, ledgerNo={}", 
                rechargeId, bonusAmount, result.getLedgerNo());
        
        // 标记成功
        execLog.markSuccess(bonusAmount, result.getLedgerNo());
    }
    
    /**
     * 计算赠送金额
     */
    private BigDecimal calculateBonusAmount(BigDecimal rechargeAmount, CampaignRulesDTO rules) {
        BigDecimal bonus = BigDecimal.ZERO;
        
        // 固定赠送金额
        if (rules.getBonusAmount() != null) {
            bonus = rules.getBonusAmount();
        }
        
        // 按比例赠送
        if (rules.getBonusRate() != null) {
            BigDecimal rateBonus = rechargeAmount.multiply(rules.getBonusRate())
                    .setScale(2, RoundingMode.DOWN);
            bonus = bonus.add(rateBonus);
        }
        
        // 赠送封顶
        if (rules.getMaxBonusAmount() != null && bonus.compareTo(rules.getMaxBonusAmount()) > 0) {
            bonus = rules.getMaxBonusAmount();
        }
        
        return bonus;
    }
    
    /**
     * 构建幂等键
     * 格式：{tenantId}:{campaignType}:{bizOrderId}:{userId}
     */
    private String buildIdempotencyKey(Long tenantId, CampaignType campaignType, 
                                       Long bizOrderId, Long userId) {
        return String.format("%d:%s:%d:%d", tenantId, campaignType.name(), bizOrderId, userId);
    }
    
    /**
     * 发送活动执行成功通知
     */
    private void sendSuccessNotification(Campaign campaign, Long tenantId, Long userId, 
                                        Long bizOrderId, ExecutionLog execLog) {
        // 如果没有注入 NotificationFacade，跳过通知
        if (notificationFacade == null) {
            log.debug("[campaign-exec] NotificationFacade 未注入，跳过通知发送");
            return;
        }
        
        try {
            String templateCode = getNotificationTemplateCode(campaign.getCampaignType());
            if (templateCode == null) {
                log.debug("[campaign-exec] 活动类型 {} 无需发送通知", campaign.getCampaignType());
                return;
            }
            
            Map<String, Object> variables = new HashMap<>();
            variables.put("campaignName", campaign.getCampaignName());
            variables.put("campaignType", campaign.getCampaignType().getDescription());
            variables.put("rewardAmount", execLog.getRewardAmount());
            variables.put("rewardResultId", execLog.getRewardResultId());
            variables.put("executedAt", execLog.getExecutedAt());
            
            EnqueueNotificationRequest notifyRequest = EnqueueNotificationRequest.builder()
                    .bizType("CAMPAIGN")
                    .bizId(campaign.getCampaignCode() + ":" + bizOrderId)
                    .tenantId(tenantId)
                    .userId(userId)
                    .templateCode(templateCode)
                    .channels(List.of(NotificationChannel.WECHAT))
                    .variables(variables)
                    .priority(50)
                    .build();
            
            notificationFacade.enqueue(notifyRequest);
            
            log.info("[campaign-exec] 活动通知已入队，campaign={}, user={}, templateCode={}", 
                    campaign.getCampaignCode(), userId, templateCode);
                    
        } catch (Exception e) {
            // 通知失败不影响活动执行，只记录日志
            log.error("[campaign-exec] 发送活动通知失败，campaign={}, user={}", 
                    campaign.getCampaignCode(), userId, e);
        }
    }
    
    /**
     * 获取通知模板编码
     */
    private String getNotificationTemplateCode(CampaignType campaignType) {
        return switch (campaignType) {
            case ORDER_REBATE_COUPON -> "CAMPAIGN_COUPON_REBATE";
            case RECHARGE_BONUS -> "CAMPAIGN_RECHARGE_BONUS";
            case ORDER_DISCOUNT -> null; // 计价类活动不发通知
        };
    }
    
    /**
     * 计算订单满减金额（用于计价阶段）
     * 
     * @param orderAmount 订单金额
     * @param rules 活动规则
     * @return 优惠金额
     */
    public BigDecimal calculateOrderDiscount(BigDecimal orderAmount, CampaignRulesDTO rules) {
        if (rules == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal discount = BigDecimal.ZERO;
        
        // 固定满减金额
        if (rules.getDiscountAmount() != null) {
            discount = rules.getDiscountAmount();
        }
        
        // 折扣率
        if (rules.getDiscountRate() != null) {
            BigDecimal rateDiscount = orderAmount.multiply(BigDecimal.ONE.subtract(rules.getDiscountRate()))
                    .setScale(2, RoundingMode.DOWN);
            discount = discount.add(rateDiscount);
        }
        
        // 封顶
        if (rules.getMaxDiscountAmount() != null && discount.compareTo(rules.getMaxDiscountAmount()) > 0) {
            discount = rules.getMaxDiscountAmount();
        }
        
        // 优惠不能超过订单金额
        if (discount.compareTo(orderAmount) > 0) {
            discount = orderAmount;
        }
        
        return discount;
    }
}
