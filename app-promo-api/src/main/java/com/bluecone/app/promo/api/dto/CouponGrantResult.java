package com.bluecone.app.promo.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 券发放结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponGrantResult {

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 券ID（发放成功时返回）
     */
    private Long couponId;

    /**
     * 错误消息（发放失败时返回）
     */
    private String errorMessage;
}
