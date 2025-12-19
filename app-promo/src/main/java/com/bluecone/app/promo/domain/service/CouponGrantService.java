package com.bluecone.app.promo.domain.service;

import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.id.api.IdScope;
import com.bluecone.app.id.api.IdService;
import com.bluecone.app.promo.api.enums.CouponStatus;
import com.bluecone.app.promo.api.enums.GrantSource;
import com.bluecone.app.promo.api.enums.GrantStatus;
import com.bluecone.app.promo.domain.model.Coupon;
import com.bluecone.app.promo.domain.model.CouponGrantLog;
import com.bluecone.app.promo.domain.model.CouponTemplate;
import com.bluecone.app.promo.domain.repository.CouponGrantLogRepository;
import com.bluecone.app.promo.domain.repository.CouponRepository;
import com.bluecone.app.promo.domain.repository.CouponTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 优惠券发放服务
 * 负责优惠券发放的核心逻辑：幂等控制、配额校验、券生成
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponGrantService {

    private final CouponTemplateRepository templateRepository;
    private final CouponGrantLogRepository grantLogRepository;
    private final CouponRepository couponRepository;
    private final IdService idService;
    private final CouponGrantMetrics metrics;

    /**
     * 发放优惠券（单用户）
     * 
     * @param tenantId 租户ID
     * @param templateId 模板ID
     * @param userId 用户ID
     * @param idempotencyKey 幂等键（必填）
     * @param grantSource 发放来源
     * @param operatorId 操作人ID（可选）
     * @param operatorName 操作人名称（可选）
     * @param grantReason 发放原因（可选）
     * @return 发放的优惠券
     */
    @Transactional
    public Coupon grantCoupon(Long tenantId, 
                             Long templateId, 
                             Long userId, 
                             String idempotencyKey,
                             GrantSource grantSource,
                             Long operatorId,
                             String operatorName,
                             String grantReason) {
        
        long startTime = System.currentTimeMillis();
        
        try {
            return doGrantCoupon(tenantId, templateId, userId, idempotencyKey, 
                    grantSource, operatorId, operatorName, grantReason);
        } finally {
            metrics.recordDuration(startTime);
        }
    }

    private Coupon doGrantCoupon(Long tenantId, 
                                 Long templateId, 
                                 Long userId, 
                                 String idempotencyKey,
                                 GrantSource grantSource,
                                 Long operatorId,
                                 String operatorName,
                                 String grantReason) {
        
        // 1. 幂等检查：先查询是否已经发放过
        Optional<CouponGrantLog> existingLog = grantLogRepository.findByIdempotencyKey(tenantId, idempotencyKey);
        if (existingLog.isPresent()) {
            CouponGrantLog grantLog = existingLog.get();
            if (grantLog.isSuccess() && grantLog.getCouponId() != null) {
                // 已经成功发放，直接返回
                metrics.recordIdempotentReplay(idempotencyKey);
                log.info("优惠券发放幂等返回: idempotencyKey={}, couponId={}", idempotencyKey, grantLog.getCouponId());
                Coupon existingCoupon = couponRepository.findById(tenantId, grantLog.getCouponId());
                if (existingCoupon == null) {
                    throw new BusinessException("COUPON_NOT_FOUND", "券不存在");
                }
                return existingCoupon;
            } else if (grantLog.isFailed()) {
                // 之前失败过，返回失败信息
                throw new BusinessException(grantLog.getErrorCode(), grantLog.getErrorMessage());
            } else {
                // 处理中，可能是并发请求
                throw new BusinessException("GRANT_IN_PROGRESS", "发券请求处理中，请稍后查询");
            }
        }

        // 2. 创建发放日志（PROCESSING状态）- 利用数据库唯一约束实现幂等
        CouponGrantLog grantLog = CouponGrantLog.builder()
                .id(idService.nextLong(IdScope.COUPON_GRANT_LOG))
                .tenantId(tenantId)
                .templateId(templateId)
                .idempotencyKey(idempotencyKey)
                .userId(userId)
                .grantSource(grantSource)
                .grantStatus(GrantStatus.PROCESSING)
                .operatorId(operatorId)
                .operatorName(operatorName)
                .grantReason(grantReason)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        try {
            grantLog = grantLogRepository.save(grantLog);
        } catch (DuplicateKeyException e) {
            // 并发情况下，唯一约束冲突，说明另一个请求正在处理
            log.warn("发券幂等键冲突: idempotencyKey={}", idempotencyKey);
            throw new BusinessException("DUPLICATE_GRANT_REQUEST", "重复的发券请求");
        }

        try {
            // 3. 查询模板
            CouponTemplate template = templateRepository.findById(templateId)
                    .orElseThrow(() -> new BusinessException("TEMPLATE_NOT_FOUND", "模板不存在"));

            // 4. 校验模板状态
            if (!template.isOnline()) {
                throw new BusinessException("TEMPLATE_NOT_ONLINE", 
                        "模板未上线，当前状态: " + template.getStatus());
            }

            // 5. 校验有效期
            if (template.useFixedValidity()) {
                LocalDateTime now = LocalDateTime.now();
                if (now.isBefore(template.getValidStartTime()) || now.isAfter(template.getValidEndTime())) {
                    throw new BusinessException("TEMPLATE_NOT_IN_VALIDITY", "模板不在有效期内");
                }
            }

            // 6. 校验用户配额
            int userGrantedCount = grantLogRepository.countUserGrantedByTemplate(tenantId, templateId, userId);
            if (!template.canUserReceive(userGrantedCount)) {
                metrics.recordUserQuotaExceeded(userId, templateId);
                throw new BusinessException("USER_QUOTA_EXCEEDED", 
                        String.format("用户已达领取上限，已领取: %d, 限制: %d", 
                                userGrantedCount, template.getPerUserLimit()));
            }

            // 7. 原子扣减总配额（使用数据库原子更新）
            boolean quotaDeducted = templateRepository.incrementIssuedCount(templateId, 1);
            if (!quotaDeducted) {
                metrics.recordQuotaExceeded(templateId);
                throw new BusinessException("TOTAL_QUOTA_EXCEEDED", "模板配额已用完");
            }

            // 8. 生成优惠券实例
            Coupon coupon = createCouponFromTemplate(template, userId, grantLog.getId());
            couponRepository.save(coupon);

            // 9. 更新发放日志为成功
            grantLog.markSuccess(coupon.getId());
            grantLogRepository.update(grantLog);

            metrics.recordSuccess();
            log.info("优惠券发放成功: templateId={}, userId={}, couponId={}, idempotencyKey={}", 
                    templateId, userId, coupon.getId(), idempotencyKey);

            return coupon;

        } catch (BusinessException e) {
            // 业务异常，记录失败原因
            metrics.recordFailure(e.getCode());
            grantLog.markFailed(e.getCode(), e.getMessage());
            grantLogRepository.update(grantLog);
            throw e;
        } catch (Exception e) {
            // 系统异常
            metrics.recordFailure("SYSTEM_ERROR");
            grantLog.markFailed("SYSTEM_ERROR", "系统错误: " + e.getMessage());
            grantLogRepository.update(grantLog);
            log.error("优惠券发放失败: templateId={}, userId={}", templateId, userId, e);
            throw new BusinessException("GRANT_FAILED", "发券失败: " + e.getMessage());
        }
    }

    /**
     * 批量发放优惠券
     * 
     * @param tenantId 租户ID
     * @param templateId 模板ID
     * @param userIds 用户ID列表
     * @param batchNo 批次号
     * @param grantSource 发放来源
     * @param operatorId 操作人ID
     * @param operatorName 操作人名称
     * @param grantReason 发放原因
     * @return 发放结果列表
     */
    @Transactional
    public List<GrantResult> grantCouponBatch(Long tenantId,
                                             Long templateId,
                                             List<Long> userIds,
                                             String batchNo,
                                             GrantSource grantSource,
                                             Long operatorId,
                                             String operatorName,
                                             String grantReason) {
        
        List<GrantResult> results = new ArrayList<>();
        
        for (Long userId : userIds) {
            // 为每个用户生成唯一的幂等键
            String idempotencyKey = String.format("%s:%s:%d", batchNo, templateId, userId);
            
            try {
                Coupon coupon = grantCoupon(tenantId, templateId, userId, idempotencyKey, 
                        grantSource, operatorId, operatorName, grantReason);
                results.add(new GrantResult(userId, true, coupon.getId(), null));
            } catch (BusinessException e) {
                log.warn("批量发券失败: userId={}, error={}", userId, e.getMessage());
                results.add(new GrantResult(userId, false, null, e.getMessage()));
            }
        }
        
        log.info("批量发券完成: batchNo={}, total={}, success={}, failed={}", 
                batchNo, userIds.size(), 
                results.stream().filter(r -> r.success).count(),
                results.stream().filter(r -> !r.success).count());
        
        return results;
    }

    /**
     * 从模板创建优惠券实例
     */
    private Coupon createCouponFromTemplate(CouponTemplate template, Long userId, Long grantLogId) {
        LocalDateTime now = LocalDateTime.now();
        
        // 计算有效期
        LocalDateTime validStartTime;
        LocalDateTime validEndTime;
        
        if (template.useFixedValidity()) {
            // 使用固定有效期
            validStartTime = template.getValidStartTime();
            validEndTime = template.getValidEndTime();
        } else {
            // 使用相对有效期（从发放时间开始计算）
            validStartTime = now;
            validEndTime = now.plusDays(template.getValidDays());
        }

        // 生成券码（唯一）
        String couponCode = generateCouponCode(template.getTenantId(), template.getId());

        return Coupon.builder()
                .id(idService.nextLong(IdScope.COUPON))
                .tenantId(template.getTenantId())
                .templateId(template.getId())
                .grantLogId(grantLogId)
                .couponCode(couponCode)
                .userId(userId)
                .couponType(template.getCouponType())
                .discountAmount(template.getDiscountAmount())
                .discountRate(template.getDiscountRate())
                .minOrderAmount(template.getMinOrderAmount())
                .maxDiscountAmount(template.getMaxDiscountAmount())
                .applicableScope(template.getApplicableScope())
                .applicableScopeIds(template.getApplicableScopeIds())
                .validStartTime(validStartTime)
                .validEndTime(validEndTime)
                .status(CouponStatus.ISSUED)
                .grantTime(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    /**
     * 生成券码
     */
    private String generateCouponCode(Long tenantId, Long templateId) {
        // 格式：CPN-{tenantId}-{templateId}-{uuid前8位}
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return String.format("CPN-%d-%d-%s", tenantId, templateId, uuid);
    }

    /**
     * 发放结果
     */
    public static class GrantResult {
        public final Long userId;
        public final boolean success;
        public final Long couponId;
        public final String errorMessage;

        public GrantResult(Long userId, boolean success, Long couponId, String errorMessage) {
            this.userId = userId;
            this.success = success;
            this.couponId = couponId;
            this.errorMessage = errorMessage;
        }
    }
}
