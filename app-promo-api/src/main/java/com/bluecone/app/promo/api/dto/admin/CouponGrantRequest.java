package com.bluecone.app.promo.api.dto.admin;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 手动发券请求
 */
@Data
public class CouponGrantRequest {

    @NotNull(message = "模板ID不能为空")
    private Long templateId;

    @NotEmpty(message = "用户ID列表不能为空")
    private List<Long> userIds;

    @NotBlank(message = "幂等键不能为空")
    private String idempotencyKey;

    /**
     * 批次号（批量发券时使用）
     */
    private String batchNo;

    /**
     * 发放原因/备注
     */
    private String grantReason;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人名称
     */
    private String operatorName;
}
