package com.bluecone.app.controller.admin;

import com.bluecone.app.promo.api.dto.admin.CouponGrantLogView;
import com.bluecone.app.promo.api.dto.admin.CouponGrantRequest;
import com.bluecone.app.promo.api.dto.admin.CouponGrantResponse;
import com.bluecone.app.promo.api.enums.GrantSource;
import com.bluecone.app.promo.domain.model.Coupon;
import com.bluecone.app.promo.domain.model.CouponGrantLog;
import com.bluecone.app.promo.domain.repository.CouponGrantLogRepository;
import com.bluecone.app.promo.domain.repository.CouponRepository;
import com.bluecone.app.promo.domain.repository.CouponTemplateRepository;
import com.bluecone.app.promo.domain.service.CouponGrantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 优惠券发放管理接口（后台）
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/promo/grants")
@RequiredArgsConstructor
public class CouponGrantAdminController {

    private final CouponGrantService grantService;
    private final CouponGrantLogRepository grantLogRepository;
    private final CouponRepository couponRepository;
    private final CouponTemplateRepository templateRepository;

    /**
     * 手动发券（支持单用户或批量）
     */
    @PostMapping
    public CouponGrantResponse grantCoupons(@RequestHeader("X-Tenant-Id") Long tenantId,
                                           @Valid @RequestBody CouponGrantRequest request) {
        log.info("管理员手动发券: tenantId={}, templateId={}, userCount={}", 
                tenantId, request.getTemplateId(), request.getUserIds().size());

        // 生成批次号（如果没有提供）
        String batchNo = StringUtils.hasText(request.getBatchNo()) ? 
                request.getBatchNo() : "BATCH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        List<CouponGrantService.GrantResult> results;
        
        if (request.getUserIds().size() == 1) {
            // 单用户发券
            Long userId = request.getUserIds().get(0);
            try {
                Coupon coupon = grantService.grantCoupon(
                        tenantId,
                        request.getTemplateId(),
                        userId,
                        request.getIdempotencyKey(),
                        GrantSource.MANUAL_ADMIN,
                        request.getOperatorId(),
                        request.getOperatorName(),
                        request.getGrantReason()
                );
                results = List.of(new CouponGrantService.GrantResult(userId, true, coupon.getId(), null));
            } catch (Exception e) {
                log.error("单用户发券失败: userId={}", userId, e);
                results = List.of(new CouponGrantService.GrantResult(userId, false, null, e.getMessage()));
            }
        } else {
            // 批量发券
            results = grantService.grantCouponBatch(
                    tenantId,
                    request.getTemplateId(),
                    request.getUserIds(),
                    batchNo,
                    GrantSource.MANUAL_ADMIN,
                    request.getOperatorId(),
                    request.getOperatorName(),
                    request.getGrantReason()
            );
        }

        // 统计结果
        long successCount = results.stream().filter(r -> r.success).count();
        long failedCount = results.stream().filter(r -> !r.success).count();

        List<CouponGrantResponse.GrantResultItem> resultItems = results.stream()
                .map(r -> CouponGrantResponse.GrantResultItem.builder()
                        .userId(r.userId)
                        .success(r.success)
                        .couponId(r.couponId)
                        .errorMessage(r.errorMessage)
                        .build())
                .collect(Collectors.toList());

        return CouponGrantResponse.builder()
                .total(request.getUserIds().size())
                .successCount((int) successCount)
                .failedCount((int) failedCount)
                .results(resultItems)
                .build();
    }

    /**
     * 查询用户的发放日志
     */
    @GetMapping("/user/{userId}")
    public List<CouponGrantLogView> getUserGrantLogs(@RequestHeader("X-Tenant-Id") Long tenantId,
                                                     @PathVariable Long userId,
                                                     @RequestParam(defaultValue = "50") int limit) {
        List<CouponGrantLog> logs = grantLogRepository.findByUser(tenantId, userId, limit);
        return logs.stream()
                .map(this::toView)
                .collect(Collectors.toList());
    }

    /**
     * 查询模板的发放日志
     */
    @GetMapping("/template/{templateId}")
    public List<CouponGrantLogView> getTemplateGrantLogs(@RequestHeader("X-Tenant-Id") Long tenantId,
                                                         @PathVariable Long templateId,
                                                         @RequestParam(defaultValue = "50") int limit) {
        List<CouponGrantLog> logs = grantLogRepository.findByTemplate(tenantId, templateId, limit);
        return logs.stream()
                .map(this::toView)
                .collect(Collectors.toList());
    }

    /**
     * 根据幂等键查询发放日志
     */
    @GetMapping("/idempotency/{idempotencyKey}")
    public CouponGrantLogView getGrantLogByIdempotencyKey(@RequestHeader("X-Tenant-Id") Long tenantId,
                                                          @PathVariable String idempotencyKey) {
        CouponGrantLog log = grantLogRepository.findByIdempotencyKey(tenantId, idempotencyKey)
                .orElseThrow(() -> new IllegalArgumentException("发放日志不存在"));
        return toView(log);
    }

    /**
     * 转换为视图对象
     */
    private CouponGrantLogView toView(CouponGrantLog log) {
        CouponGrantLogView view = CouponGrantLogView.builder()
                .id(log.getId())
                .tenantId(log.getTenantId())
                .templateId(log.getTemplateId())
                .idempotencyKey(log.getIdempotencyKey())
                .userId(log.getUserId())
                .couponId(log.getCouponId())
                .grantSource(log.getGrantSource())
                .grantStatus(log.getGrantStatus())
                .operatorId(log.getOperatorId())
                .operatorName(log.getOperatorName())
                .batchNo(log.getBatchNo())
                .grantReason(log.getGrantReason())
                .errorCode(log.getErrorCode())
                .errorMessage(log.getErrorMessage())
                .createdAt(log.getCreatedAt())
                .updatedAt(log.getUpdatedAt())
                .build();

        // 填充模板名称
        templateRepository.findById(log.getTemplateId()).ifPresent(template -> {
            view.setTemplateName(template.getTemplateName());
        });

        // 填充券码
        if (log.getCouponId() != null) {
            Coupon coupon = couponRepository.findById(log.getTenantId(), log.getCouponId());
            if (coupon != null) {
                view.setCouponCode(coupon.getCouponCode());
            }
        }

        return view;
    }
}
