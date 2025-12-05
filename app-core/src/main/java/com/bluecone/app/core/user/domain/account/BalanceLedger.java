package com.bluecone.app.core.user.domain.account;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * 储值流水领域对象，对应表 bc_member_balance_ledger。
 */
@Data
public class BalanceLedger {

    private Long id;

    private Long tenantId;

    private Long memberId;

    private String bizType;

    private String bizId;

    private BigDecimal changeAmount;

    private BigDecimal balanceAfter;

    private String remark;

    private LocalDateTime occurredAt;

    private LocalDateTime createdAt;
}
