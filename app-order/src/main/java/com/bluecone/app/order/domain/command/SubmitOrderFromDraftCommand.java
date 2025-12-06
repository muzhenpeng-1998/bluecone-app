package com.bluecone.app.order.domain.command;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

/**
 * 从草稿确认订单的领域命令。
 */
@Data
@Builder
public class SubmitOrderFromDraftCommand {

    private final String orderToken;

    private final BigDecimal clientPayableAmount;

    private final String userRemark;

    private final String contactName;

    private final String contactPhone;

    private final String addressJson;
}
