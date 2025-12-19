package com.bluecone.app.promo.infra.persistence.converter;

import com.bluecone.app.promo.api.enums.ApplicableScope;
import com.bluecone.app.promo.api.enums.CouponStatus;
import com.bluecone.app.promo.api.enums.CouponType;
import com.bluecone.app.promo.api.enums.LockStatus;
import com.bluecone.app.promo.domain.model.Coupon;
import com.bluecone.app.promo.domain.model.CouponLock;
import com.bluecone.app.promo.domain.model.CouponRedemption;
import com.bluecone.app.promo.domain.model.CouponTemplate;
import com.bluecone.app.promo.infra.persistence.po.CouponLockPO;
import com.bluecone.app.promo.infra.persistence.po.CouponPO;
import com.bluecone.app.promo.infra.persistence.po.CouponRedemptionPO;
import com.bluecone.app.promo.infra.persistence.po.CouponTemplatePO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;

/**
 * 优惠券模型转换器
 */
public class CouponConverter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private CouponConverter() {
    }

    // ==================== CouponTemplate ====================

    public static CouponTemplate toDomain(CouponTemplatePO po) {
        if (po == null) {
            return null;
        }
        return CouponTemplate.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .templateCode(po.getTemplateCode())
                .templateName(po.getTemplateName())
                .couponType(parseCouponType(po.getCouponType()))
                .discountAmount(po.getDiscountAmount())
                .discountRate(po.getDiscountRate())
                .minOrderAmount(po.getMinOrderAmount())
                .maxDiscountAmount(po.getMaxDiscountAmount())
                .applicableScope(parseApplicableScope(po.getApplicableScope()))
                .applicableScopeIds(parseJsonList(po.getApplicableScopeIds()))
                .validDays(po.getValidDays())
                .validStartTime(po.getValidStartTime())
                .validEndTime(po.getValidEndTime())
                .totalQuantity(po.getTotalQuantity())
                .perUserLimit(po.getPerUserLimit())
                .status(po.getStatus())
                .description(po.getDescription())
                .termsOfUse(po.getTermsOfUse())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    public static CouponTemplatePO toPO(CouponTemplate template) {
        if (template == null) {
            return null;
        }
        CouponTemplatePO po = new CouponTemplatePO();
        po.setId(template.getId());
        po.setTenantId(template.getTenantId());
        po.setTemplateCode(template.getTemplateCode());
        po.setTemplateName(template.getTemplateName());
        po.setCouponType(template.getCouponType() != null ? template.getCouponType().name() : null);
        po.setDiscountAmount(template.getDiscountAmount());
        po.setDiscountRate(template.getDiscountRate());
        po.setMinOrderAmount(template.getMinOrderAmount());
        po.setMaxDiscountAmount(template.getMaxDiscountAmount());
        po.setApplicableScope(template.getApplicableScope() != null ? template.getApplicableScope().name() : null);
        po.setApplicableScopeIds(toJsonString(template.getApplicableScopeIds()));
        po.setValidDays(template.getValidDays());
        po.setValidStartTime(template.getValidStartTime());
        po.setValidEndTime(template.getValidEndTime());
        po.setTotalQuantity(template.getTotalQuantity());
        po.setPerUserLimit(template.getPerUserLimit());
        po.setStatus(template.getStatus());
        po.setDescription(template.getDescription());
        po.setTermsOfUse(template.getTermsOfUse());
        po.setCreatedAt(template.getCreatedAt());
        po.setUpdatedAt(template.getUpdatedAt());
        return po;
    }

    // ==================== Coupon ====================

    public static Coupon toDomain(CouponPO po) {
        if (po == null) {
            return null;
        }
        return Coupon.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .templateId(po.getTemplateId())
                .couponCode(po.getCouponCode())
                .userId(po.getUserId())
                .couponType(parseCouponType(po.getCouponType()))
                .discountAmount(po.getDiscountAmount())
                .discountRate(po.getDiscountRate())
                .minOrderAmount(po.getMinOrderAmount())
                .maxDiscountAmount(po.getMaxDiscountAmount())
                .applicableScope(parseApplicableScope(po.getApplicableScope()))
                .applicableScopeIds(parseJsonList(po.getApplicableScopeIds()))
                .validStartTime(po.getValidStartTime())
                .validEndTime(po.getValidEndTime())
                .status(parseCouponStatus(po.getStatus()))
                .grantTime(po.getGrantTime())
                .lockTime(po.getLockTime())
                .useTime(po.getUseTime())
                .orderId(po.getOrderId())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    public static CouponPO toPO(Coupon coupon) {
        if (coupon == null) {
            return null;
        }
        CouponPO po = new CouponPO();
        po.setId(coupon.getId());
        po.setTenantId(coupon.getTenantId());
        po.setTemplateId(coupon.getTemplateId());
        po.setCouponCode(coupon.getCouponCode());
        po.setUserId(coupon.getUserId());
        po.setCouponType(coupon.getCouponType() != null ? coupon.getCouponType().name() : null);
        po.setDiscountAmount(coupon.getDiscountAmount());
        po.setDiscountRate(coupon.getDiscountRate());
        po.setMinOrderAmount(coupon.getMinOrderAmount());
        po.setMaxDiscountAmount(coupon.getMaxDiscountAmount());
        po.setApplicableScope(coupon.getApplicableScope() != null ? coupon.getApplicableScope().name() : null);
        po.setApplicableScopeIds(toJsonString(coupon.getApplicableScopeIds()));
        po.setValidStartTime(coupon.getValidStartTime());
        po.setValidEndTime(coupon.getValidEndTime());
        po.setStatus(coupon.getStatus() != null ? coupon.getStatus().name() : null);
        po.setGrantTime(coupon.getGrantTime());
        po.setLockTime(coupon.getLockTime());
        po.setUseTime(coupon.getUseTime());
        po.setOrderId(coupon.getOrderId());
        po.setCreatedAt(coupon.getCreatedAt());
        po.setUpdatedAt(coupon.getUpdatedAt());
        return po;
    }

    // ==================== CouponLock ====================

    public static CouponLock toDomain(CouponLockPO po) {
        if (po == null) {
            return null;
        }
        return CouponLock.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .couponId(po.getCouponId())
                .userId(po.getUserId())
                .orderId(po.getOrderId())
                .idempotencyKey(po.getIdempotencyKey())
                .lockStatus(parseLockStatus(po.getLockStatus()))
                .lockTime(po.getLockTime())
                .releaseTime(po.getReleaseTime())
                .commitTime(po.getCommitTime())
                .expireTime(po.getExpireTime())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    public static CouponLockPO toPO(CouponLock lock) {
        if (lock == null) {
            return null;
        }
        CouponLockPO po = new CouponLockPO();
        po.setId(lock.getId());
        po.setTenantId(lock.getTenantId());
        po.setCouponId(lock.getCouponId());
        po.setUserId(lock.getUserId());
        po.setOrderId(lock.getOrderId());
        po.setIdempotencyKey(lock.getIdempotencyKey());
        po.setLockStatus(lock.getLockStatus() != null ? lock.getLockStatus().name() : null);
        po.setLockTime(lock.getLockTime());
        po.setReleaseTime(lock.getReleaseTime());
        po.setCommitTime(lock.getCommitTime());
        po.setExpireTime(lock.getExpireTime());
        po.setCreatedAt(lock.getCreatedAt());
        po.setUpdatedAt(lock.getUpdatedAt());
        return po;
    }

    // ==================== CouponRedemption ====================

    public static CouponRedemption toDomain(CouponRedemptionPO po) {
        if (po == null) {
            return null;
        }
        return CouponRedemption.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .couponId(po.getCouponId())
                .templateId(po.getTemplateId())
                .userId(po.getUserId())
                .orderId(po.getOrderId())
                .idempotencyKey(po.getIdempotencyKey())
                .originalAmount(po.getOriginalAmount())
                .discountAmount(po.getDiscountAmount())
                .finalAmount(po.getFinalAmount())
                .redemptionTime(po.getRedemptionTime())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    public static CouponRedemptionPO toPO(CouponRedemption redemption) {
        if (redemption == null) {
            return null;
        }
        CouponRedemptionPO po = new CouponRedemptionPO();
        po.setId(redemption.getId());
        po.setTenantId(redemption.getTenantId());
        po.setCouponId(redemption.getCouponId());
        po.setTemplateId(redemption.getTemplateId());
        po.setUserId(redemption.getUserId());
        po.setOrderId(redemption.getOrderId());
        po.setIdempotencyKey(redemption.getIdempotencyKey());
        po.setOriginalAmount(redemption.getOriginalAmount());
        po.setDiscountAmount(redemption.getDiscountAmount());
        po.setFinalAmount(redemption.getFinalAmount());
        po.setRedemptionTime(redemption.getRedemptionTime());
        po.setCreatedAt(redemption.getCreatedAt());
        po.setUpdatedAt(redemption.getUpdatedAt());
        return po;
    }

    // ==================== Helper Methods ====================

    private static CouponType parseCouponType(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        try {
            return CouponType.valueOf(code);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static CouponStatus parseCouponStatus(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        try {
            return CouponStatus.valueOf(code);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static ApplicableScope parseApplicableScope(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        try {
            return ApplicableScope.valueOf(code);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static LockStatus parseLockStatus(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        try {
            return LockStatus.valueOf(code);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static List<Long> parseJsonList(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<List<Long>>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    private static String toJsonString(List<Long> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
