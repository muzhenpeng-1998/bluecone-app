package com.bluecone.app.promo.domain.model;

import com.bluecone.app.promo.api.enums.GrantSource;
import com.bluecone.app.promo.api.enums.GrantStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 优惠券发放日志领域模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponGrantLog implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private Long templateId;
    private String idempotencyKey;
    private Long userId;
    private Long couponId;
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

    /**
     * 标记为成功
     */
    public void markSuccess(Long couponId) {
        this.grantStatus = GrantStatus.SUCCESS;
        this.couponId = couponId;
    }

    /**
     * 标记为失败
     */
    public void markFailed(String errorCode, String errorMessage) {
        this.grantStatus = GrantStatus.FAILED;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    /**
     * 是否成功
     */
    public boolean isSuccess() {
        return grantStatus == GrantStatus.SUCCESS;
    }

    /**
     * 是否失败
     */
    public boolean isFailed() {
        return grantStatus == GrantStatus.FAILED;
    }

    /**
     * 是否处理中
     */
    public boolean isProcessing() {
        return grantStatus == GrantStatus.PROCESSING;
    }
}
