package com.bluecone.app.order.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户订单列表查询参数。
 */
@Data
public class UserOrderListQuery {

    @NotNull(message = "tenantId 不能为空")
    private Long tenantId;

    @NotNull(message = "userId 不能为空")
    private Long userId;

    /**
     * 订单状态，逗号分隔。
     */
    private String status;

    /**
     * 开始时间（yyyy-MM-dd HH:mm:ss）。
     */
    private String fromTime;

    /**
     * 结束时间（yyyy-MM-dd HH:mm:ss）。
     */
    private String toTime;

    @Min(value = 1, message = "pageNo 最小为 1")
    private Integer pageNo = 1;

    @Min(value = 1, message = "pageSize 最小为 1")
    @Max(value = 100, message = "pageSize 最大为 100")
    private Integer pageSize = 20;
}
