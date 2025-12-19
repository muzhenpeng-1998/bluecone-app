package com.bluecone.app.promo.infra.persistence.converter;

import com.bluecone.app.promo.api.enums.GrantSource;
import com.bluecone.app.promo.api.enums.GrantStatus;
import com.bluecone.app.promo.domain.model.CouponGrantLog;
import com.bluecone.app.promo.infra.persistence.po.CouponGrantLogPO;
import org.springframework.stereotype.Component;

/**
 * 优惠券发放日志转换器
 */
@Component
public class CouponGrantLogConverter {

    public CouponGrantLogPO toPO(CouponGrantLog domain) {
        if (domain == null) {
            return null;
        }

        CouponGrantLogPO po = new CouponGrantLogPO();
        po.setId(domain.getId());
        po.setTenantId(domain.getTenantId());
        po.setTemplateId(domain.getTemplateId());
        po.setIdempotencyKey(domain.getIdempotencyKey());
        po.setUserId(domain.getUserId());
        po.setCouponId(domain.getCouponId());
        po.setGrantSource(domain.getGrantSource() != null ? domain.getGrantSource().name() : null);
        po.setGrantStatus(domain.getGrantStatus() != null ? domain.getGrantStatus().name() : null);
        po.setOperatorId(domain.getOperatorId());
        po.setOperatorName(domain.getOperatorName());
        po.setBatchNo(domain.getBatchNo());
        po.setGrantReason(domain.getGrantReason());
        po.setErrorCode(domain.getErrorCode());
        po.setErrorMessage(domain.getErrorMessage());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());

        return po;
    }

    public CouponGrantLog toDomain(CouponGrantLogPO po) {
        if (po == null) {
            return null;
        }

        return CouponGrantLog.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .templateId(po.getTemplateId())
                .idempotencyKey(po.getIdempotencyKey())
                .userId(po.getUserId())
                .couponId(po.getCouponId())
                .grantSource(po.getGrantSource() != null ? GrantSource.valueOf(po.getGrantSource()) : null)
                .grantStatus(po.getGrantStatus() != null ? GrantStatus.valueOf(po.getGrantStatus()) : null)
                .operatorId(po.getOperatorId())
                .operatorName(po.getOperatorName())
                .batchNo(po.getBatchNo())
                .grantReason(po.getGrantReason())
                .errorCode(po.getErrorCode())
                .errorMessage(po.getErrorMessage())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }
}
