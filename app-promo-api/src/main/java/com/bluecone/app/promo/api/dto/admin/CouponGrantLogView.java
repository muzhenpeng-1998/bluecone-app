package com.bluecone.app.promo.api.dto.admin;

import com.bluecone.app.promo.api.enums.GrantSource;
import com.bluecone.app.promo.api.enums.GrantStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 优惠券发放日志视图
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponGrantLogView {

    private Long id;
    private Long tenantId;
    private Long templateId;
    private String templateName;
    private String idempotencyKey;
    private Long userId;
    private Long couponId;
    private String couponCode;
    private GrantSource grantSource;
    private GrantStatus grantStatus;
    private Long operatorId;
    private String operatorName;
    private String batchNo;
    private String grantReason;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
