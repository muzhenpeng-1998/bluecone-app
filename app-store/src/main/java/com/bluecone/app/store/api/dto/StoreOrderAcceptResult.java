package com.bluecone.app.store.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 高频调用的下单可受理结果对象，字段尽量精简以降低序列化与传输成本。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreOrderAcceptResult {

    /**
     * 是否可受理订单。
     */
    private boolean acceptable;

    /**
     * 失败原因编码，例如 STORE_CLOSED、OUT_OF_BUSINESS_HOURS、CAPABILITY_DISABLED。
     */
    private String reasonCode;

    /**
     * 失败原因中文提示，便于前端展示和日志排查。
     */
    private String reasonMessage;

    /**
     * 详细错误信息，包含额外的上下文信息（如具体的营业时间、渠道信息等），用于前端展示和埋点统计。
     */
    private String detail;
}
