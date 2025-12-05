package com.bluecone.app.order.domain.model;

import com.bluecone.app.order.domain.enums.PayStatus;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPayment implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long tenantId;

    private Long storeId;

    private Long orderId;

    private String payChannel;

    private PayStatus payStatus;

    @Builder.Default
    private BigDecimal payAmount = BigDecimal.ZERO;

    @Builder.Default
    private String currency = "CNY";

    private String thirdTradeNo;

    private LocalDateTime payTime;

    @Builder.Default
    private Map<String, Object> extra = Collections.emptyMap();

    private LocalDateTime createdAt;

    private Long createdBy;

    private LocalDateTime updatedAt;

    private Long updatedBy;
}
