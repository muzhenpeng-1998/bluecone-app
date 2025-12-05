package com.bluecone.app.core.user.domain.account;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 储值账户变动结果，记录本次变动及变动后余额。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BalanceChangeResult {

    private BigDecimal changeAmount;

    private BigDecimal balanceAfter;

    private boolean success;

    private String failReason;

    public static BalanceChangeResult success(BigDecimal delta, BigDecimal balanceAfter) {
        return new BalanceChangeResult(delta, balanceAfter, true, null);
    }

    public static BalanceChangeResult idempotent(BigDecimal delta, BigDecimal balanceAfter) {
        return new BalanceChangeResult(delta, balanceAfter, true, "IDEMPOTENT");
    }

    public static BalanceChangeResult failed(String reason) {
        return new BalanceChangeResult(BigDecimal.ZERO, BigDecimal.ZERO, false, reason);
    }
}
