package com.bluecone.app.core.user.domain.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 储值账户变动事件。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BalanceChangedEvent {

    private Long tenantId;

    private Long memberId;

    private BigDecimal changeAmount;

    private BigDecimal balanceAfter;

    private String bizType;

    private String bizId;

    private LocalDateTime occurredAt;
}
