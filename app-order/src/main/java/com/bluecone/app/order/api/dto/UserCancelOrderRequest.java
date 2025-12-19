package com.bluecone.app.order.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户取消订单请求（M4 版本：支持幂等 + 乐观锁）。
 */
@Data
public class UserCancelOrderRequest {
    
    /**
     * 租户ID（TODO: 后续从登录态上下文注入）。
     */
    @NotNull(message = "tenantId 不能为空")
    private Long tenantId;
    
    /**
     * 门店ID（TODO: 后续从登录态上下文注入）。
     */
    @NotNull(message = "storeId 不能为空")
    private Long storeId;
    
    /**
     * 用户ID（TODO: 后续从登录态上下文注入）。
     */
    @NotNull(message = "userId 不能为空")
    private Long userId;
    
    /**
     * 请求ID（幂等键，前端生成UUID）。
     */
    @NotNull(message = "requestId 不能为空")
    private String requestId;
    
    /**
     * 期望版本号（乐观锁，前端从订单详情获取）。
     */
    private Integer expectedVersion;
    
    /**
     * 取消原因码（USER_CANCEL、OTHER）。
     */
    private String reasonCode;
    
    /**
     * 取消原因描述（用户填写的具体原因）。
     */
    private String reasonDesc;
}
