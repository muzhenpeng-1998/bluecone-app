package com.bluecone.app.order.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 商户侧订单列表查询参数。
 */
@Data
public class MerchantOrderListQuery {

    @NotNull(message = "tenantId 不能为空")
    private Long tenantId;

    @NotNull(message = "storeId 不能为空")
    private Long storeId;

    @NotNull(message = "operatorId 不能为空")
    private Long operatorId;

    /**
     * 订单状态过滤，逗号分隔。
     */
    private String status;

    /**
     * 订单来源：DINE_IN/TAKEAWAY/DELIVERY。
     */
    private String orderSource;

    /**
     * 下单时间开始（yyyy-MM-dd HH:mm:ss）。
     */
    private String fromTime;

    /**
     * 下单时间结束（yyyy-MM-dd HH:mm:ss）。
     */
    private String toTime;

    @Min(value = 1, message = "pageNo 最小为 1")
    private Integer pageNo = 1;

    @Min(value = 1, message = "pageSize 最小为 1")
    @Max(value = 100, message = "pageSize 最大为 100")
    private Integer pageSize = 20;
}
