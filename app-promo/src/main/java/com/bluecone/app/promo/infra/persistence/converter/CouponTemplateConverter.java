package com.bluecone.app.promo.infra.persistence.converter;

import com.bluecone.app.promo.api.enums.ApplicableScope;
import com.bluecone.app.promo.api.enums.CouponType;
import com.bluecone.app.promo.domain.model.CouponTemplate;
import com.bluecone.app.promo.infra.persistence.po.CouponTemplatePO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * 优惠券模板转换器
 */
@Component
public class CouponTemplateConverter {

    private final ObjectMapper objectMapper;

    public CouponTemplateConverter(@Qualifier("redisObjectMapper") ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public CouponTemplatePO toPO(CouponTemplate domain) {
        if (domain == null) {
            return null;
        }

        CouponTemplatePO po = new CouponTemplatePO();
        po.setId(domain.getId());
        po.setTenantId(domain.getTenantId());
        po.setTemplateCode(domain.getTemplateCode());
        po.setTemplateName(domain.getTemplateName());
        po.setCouponType(domain.getCouponType() != null ? domain.getCouponType().name() : null);
        po.setDiscountAmount(domain.getDiscountAmount());
        po.setDiscountRate(domain.getDiscountRate());
        po.setMinOrderAmount(domain.getMinOrderAmount());
        po.setMaxDiscountAmount(domain.getMaxDiscountAmount());
        po.setApplicableScope(domain.getApplicableScope() != null ? domain.getApplicableScope().name() : null);
        po.setApplicableScopeIds(toJson(domain.getApplicableScopeIds()));
        po.setValidDays(domain.getValidDays());
        po.setValidStartTime(domain.getValidStartTime());
        po.setValidEndTime(domain.getValidEndTime());
        po.setTotalQuantity(domain.getTotalQuantity());
        po.setPerUserLimit(domain.getPerUserLimit());
        po.setIssuedCount(domain.getIssuedCount());
        po.setVersion(domain.getVersion());
        po.setStatus(domain.getStatus());
        po.setDescription(domain.getDescription());
        po.setTermsOfUse(domain.getTermsOfUse());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());

        return po;
    }

    public CouponTemplate toDomain(CouponTemplatePO po) {
        if (po == null) {
            return null;
        }

        return CouponTemplate.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .templateCode(po.getTemplateCode())
                .templateName(po.getTemplateName())
                .couponType(po.getCouponType() != null ? CouponType.valueOf(po.getCouponType()) : null)
                .discountAmount(po.getDiscountAmount())
                .discountRate(po.getDiscountRate())
                .minOrderAmount(po.getMinOrderAmount())
                .maxDiscountAmount(po.getMaxDiscountAmount())
                .applicableScope(po.getApplicableScope() != null ? ApplicableScope.valueOf(po.getApplicableScope()) : null)
                .applicableScopeIds(fromJson(po.getApplicableScopeIds()))
                .validDays(po.getValidDays())
                .validStartTime(po.getValidStartTime())
                .validEndTime(po.getValidEndTime())
                .totalQuantity(po.getTotalQuantity())
                .perUserLimit(po.getPerUserLimit())
                .issuedCount(po.getIssuedCount())
                .version(po.getVersion())
                .status(po.getStatus())
                .description(po.getDescription())
                .termsOfUse(po.getTermsOfUse())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    private String toJson(List<Long> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize list to JSON", e);
        }
    }

    private List<Long> fromJson(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
