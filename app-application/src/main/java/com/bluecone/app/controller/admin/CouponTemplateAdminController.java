package com.bluecone.app.controller.admin;

import com.bluecone.app.id.api.IdScope;
import com.bluecone.app.id.api.IdService;
import com.bluecone.app.infra.admin.service.AuditLogService;
import com.bluecone.app.promo.api.dto.admin.CouponTemplateCreateRequest;
import com.bluecone.app.promo.api.dto.admin.CouponTemplateUpdateRequest;
import com.bluecone.app.promo.api.dto.admin.CouponTemplateView;
import com.bluecone.app.promo.domain.model.CouponTemplate;
import com.bluecone.app.promo.domain.repository.CouponTemplateRepository;
import com.bluecone.app.promo.domain.service.CouponTemplateDomainService;
import com.bluecone.app.security.admin.RequireAdminPermission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 优惠券模板管理接口（后台）
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/promo/templates")
@RequiredArgsConstructor
public class CouponTemplateAdminController {

    private final CouponTemplateDomainService templateDomainService;
    private final CouponTemplateRepository templateRepository;
    private final IdService idService;
    private final AuditLogService auditLogService;

    /**
     * 创建模板（草稿状态）
     */
    @PostMapping
    @RequireAdminPermission("coupon:create")
    public CouponTemplateView createTemplate(@RequestHeader("X-Tenant-Id") Long tenantId,
                                            @Valid @RequestBody CouponTemplateCreateRequest request) {
        log.info("创建优惠券模板: tenantId={}, templateCode={}", tenantId, request.getTemplateCode());

        CouponTemplate template = CouponTemplate.builder()
                .id(idService.nextLong(IdScope.COUPON_TEMPLATE))
                .tenantId(tenantId)
                .templateCode(request.getTemplateCode())
                .templateName(request.getTemplateName())
                .couponType(request.getCouponType())
                .discountAmount(request.getDiscountAmount())
                .discountRate(request.getDiscountRate())
                .minOrderAmount(request.getMinOrderAmount())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .applicableScope(request.getApplicableScope())
                .applicableScopeIds(request.getApplicableScopeIds())
                .validDays(request.getValidDays())
                .validStartTime(request.getValidStartTime())
                .validEndTime(request.getValidEndTime())
                .totalQuantity(request.getTotalQuantity())
                .perUserLimit(request.getPerUserLimit())
                .description(request.getDescription())
                .termsOfUse(request.getTermsOfUse())
                .build();

        template = templateDomainService.createDraft(template);
        
        // 记录审计日志
        Long operatorId = getCurrentUserId();
        auditLogService.log(auditLogService.builder(tenantId, operatorId)
                .action("CREATE")
                .resourceType("COUPON_TEMPLATE")
                .resourceId(template.getId())
                .resourceName(template.getTemplateName())
                .operationDesc("创建优惠券模板")
                .dataAfter(template));
        
        return toView(template);
    }

    /**
     * 更新模板（仅草稿状态可更新）
     */
    @PutMapping("/{id}")
    @RequireAdminPermission("coupon:edit")
    public CouponTemplateView updateTemplate(@RequestHeader("X-Tenant-Id") Long tenantId,
                                            @PathVariable Long id,
                                            @Valid @RequestBody CouponTemplateUpdateRequest request) {
        log.info("更新优惠券模板: tenantId={}, templateId={}", tenantId, id);

        CouponTemplate templateBefore = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("模板不存在"));

        CouponTemplate template = templateBefore;
        // 更新字段
        if (request.getTemplateName() != null) {
            template.setTemplateName(request.getTemplateName());
        }
        if (request.getCouponType() != null) {
            template.setCouponType(request.getCouponType());
        }
        if (request.getDiscountAmount() != null) {
            template.setDiscountAmount(request.getDiscountAmount());
        }
        if (request.getDiscountRate() != null) {
            template.setDiscountRate(request.getDiscountRate());
        }
        if (request.getMinOrderAmount() != null) {
            template.setMinOrderAmount(request.getMinOrderAmount());
        }
        if (request.getMaxDiscountAmount() != null) {
            template.setMaxDiscountAmount(request.getMaxDiscountAmount());
        }
        if (request.getApplicableScope() != null) {
            template.setApplicableScope(request.getApplicableScope());
        }
        if (request.getApplicableScopeIds() != null) {
            template.setApplicableScopeIds(request.getApplicableScopeIds());
        }
        if (request.getValidDays() != null) {
            template.setValidDays(request.getValidDays());
        }
        if (request.getValidStartTime() != null) {
            template.setValidStartTime(request.getValidStartTime());
        }
        if (request.getValidEndTime() != null) {
            template.setValidEndTime(request.getValidEndTime());
        }
        if (request.getTotalQuantity() != null) {
            template.setTotalQuantity(request.getTotalQuantity());
        }
        if (request.getPerUserLimit() != null) {
            template.setPerUserLimit(request.getPerUserLimit());
        }
        if (request.getDescription() != null) {
            template.setDescription(request.getDescription());
        }
        if (request.getTermsOfUse() != null) {
            template.setTermsOfUse(request.getTermsOfUse());
        }

        templateDomainService.updateDraft(template);
        
        // 重新查询返回
        CouponTemplate templateAfter = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("模板不存在"));
        
        // 记录审计日志
        Long operatorId = getCurrentUserId();
        auditLogService.log(auditLogService.builder(tenantId, operatorId)
                .action("UPDATE")
                .resourceType("COUPON_TEMPLATE")
                .resourceId(id)
                .resourceName(templateAfter.getTemplateName())
                .operationDesc("修改优惠券模板")
                .dataBefore(templateBefore)
                .dataAfter(templateAfter));
        
        return toView(templateAfter);
    }

    /**
     * 上线模板
     */
    @PostMapping("/{id}/publish")
    @RequireAdminPermission("coupon:online")
    public CouponTemplateView publishTemplate(@RequestHeader("X-Tenant-Id") Long tenantId,
                                             @PathVariable Long id) {
        log.info("上线优惠券模板: tenantId={}, templateId={}", tenantId, id);
        
        templateDomainService.publishTemplate(id);
        
        CouponTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("模板不存在"));
        
        // 记录审计日志
        Long operatorId = getCurrentUserId();
        auditLogService.log(auditLogService.builder(tenantId, operatorId)
                .action("ONLINE")
                .resourceType("COUPON_TEMPLATE")
                .resourceId(id)
                .resourceName(template.getTemplateName())
                .operationDesc("上线优惠券模板"));
        
        return toView(template);
    }

    /**
     * 下线模板
     */
    @PostMapping("/{id}/offline")
    @RequireAdminPermission("coupon:online")
    public CouponTemplateView offlineTemplate(@RequestHeader("X-Tenant-Id") Long tenantId,
                                             @PathVariable Long id,
                                             @RequestParam(required = false) String reason) {
        log.info("下线优惠券模板: tenantId={}, templateId={}, reason={}", tenantId, id, reason);
        
        templateDomainService.offlineTemplate(id, reason);
        
        CouponTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("模板不存在"));
        
        // 记录审计日志
        Long operatorId = getCurrentUserId();
        auditLogService.log(auditLogService.builder(tenantId, operatorId)
                .action("OFFLINE")
                .resourceType("COUPON_TEMPLATE")
                .resourceId(id)
                .resourceName(template.getTemplateName())
                .operationDesc("下线优惠券模板: " + (reason != null ? reason : "")));
        
        return toView(template);
    }

    /**
     * 重新上线模板
     */
    @PostMapping("/{id}/republish")
    public CouponTemplateView republishTemplate(@RequestHeader("X-Tenant-Id") Long tenantId,
                                               @PathVariable Long id) {
        log.info("重新上线优惠券模板: tenantId={}, templateId={}", tenantId, id);
        
        templateDomainService.republishTemplate(id);
        
        CouponTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("模板不存在"));
        
        return toView(template);
    }

    /**
     * 查询模板详情
     */
    @GetMapping("/{id}")
    @RequireAdminPermission("coupon:view")
    public CouponTemplateView getTemplate(@RequestHeader("X-Tenant-Id") Long tenantId,
                                         @PathVariable Long id) {
        CouponTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("模板不存在"));
        
        return toView(template);
    }

    /**
     * 查询租户的所有模板
     */
    @GetMapping
    @RequireAdminPermission("coupon:view")
    public List<CouponTemplateView> listTemplates(@RequestHeader("X-Tenant-Id") Long tenantId,
                                                  @RequestParam(required = false) String status) {
        List<CouponTemplate> templates;
        
        if ("ONLINE".equals(status)) {
            templates = templateRepository.findOnlineTemplates(tenantId);
        } else {
            templates = templateRepository.findByTenant(tenantId);
        }
        
        return templates.stream()
                .map(this::toView)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取当前操作人ID
     */
    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() != null) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof com.bluecone.app.security.core.SecurityUserPrincipal) {
                    return ((com.bluecone.app.security.core.SecurityUserPrincipal) principal).getUserId();
                }
            }
        } catch (Exception e) {
            log.error("获取当前用户ID失败", e);
        }
        return null;
    }

    /**
     * 转换为视图对象
     */
    private CouponTemplateView toView(CouponTemplate template) {
        return CouponTemplateView.builder()
                .id(template.getId())
                .tenantId(template.getTenantId())
                .templateCode(template.getTemplateCode())
                .templateName(template.getTemplateName())
                .couponType(template.getCouponType())
                .discountAmount(template.getDiscountAmount())
                .discountRate(template.getDiscountRate())
                .minOrderAmount(template.getMinOrderAmount())
                .maxDiscountAmount(template.getMaxDiscountAmount())
                .applicableScope(template.getApplicableScope())
                .applicableScopeIds(template.getApplicableScopeIds())
                .validDays(template.getValidDays())
                .validStartTime(template.getValidStartTime())
                .validEndTime(template.getValidEndTime())
                .totalQuantity(template.getTotalQuantity())
                .perUserLimit(template.getPerUserLimit())
                .issuedCount(template.getIssuedCount())
                .version(template.getVersion())
                .status(template.getStatus() != null ? 
                        com.bluecone.app.promo.api.enums.TemplateStatus.valueOf(template.getStatus()) : null)
                .description(template.getDescription())
                .termsOfUse(template.getTermsOfUse())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }
}
