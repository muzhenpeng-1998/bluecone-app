package com.bluecone.app.promo.api.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 发券响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponGrantResponse {

    /**
     * 总数
     */
    private Integer total;

    /**
     * 成功数
     */
    private Integer successCount;

    /**
     * 失败数
     */
    private Integer failedCount;

    /**
     * 详细结果
     */
    private List<GrantResultItem> results;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GrantResultItem {
        private Long userId;
        private Boolean success;
        private Long couponId;
        private String errorMessage;
    }
}
