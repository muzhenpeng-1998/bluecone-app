package com.bluecone.app.user.dto.account;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 储值流水条目 DTO，对应 bc_member_balance_ledger。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceLedgerItemDTO {

    private Long id;

    private String bizType;

    private String bizId;

    private BigDecimal changeAmount;

    private BigDecimal balanceAfter;

    private String remark;

    private String occurredAt;
}
