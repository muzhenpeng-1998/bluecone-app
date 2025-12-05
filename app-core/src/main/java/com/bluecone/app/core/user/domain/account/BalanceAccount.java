package com.bluecone.app.core.user.domain.account;

import java.math.BigDecimal;

import lombok.Data;

/**
 * 会员储值账户聚合，对应表 bc_member_balance_account。
 */
@Data
public class BalanceAccount {

    private Long id;

    private Long tenantId;

    private Long memberId;

    private BigDecimal availableAmount;

    private BigDecimal frozenAmount;

    private long version;

    /**
     * 初始化空储值账户。
     */
    public static BalanceAccount initFor(Long tenantId, Long memberId) {
        BalanceAccount account = new BalanceAccount();
        account.setTenantId(tenantId);
        account.setMemberId(memberId);
        account.setAvailableAmount(BigDecimal.ZERO);
        account.setFrozenAmount(BigDecimal.ZERO);
        account.setVersion(0);
        return account;
    }

    /**
     * 应用余额变更并递增版本。
     */
    public void applyChange(BigDecimal delta) {
        this.availableAmount = this.availableAmount.add(delta);
        this.version = this.version + 1;
    }

    /**
     * 储值充值。
     */
    public BalanceChangeResult recharge(BigDecimal amount, String bizType, String bizId, String remark) {
        throw new UnsupportedOperationException("TODO implement recharge");
    }

    /**
     * 储值消费。
     */
    public BalanceChangeResult consume(BigDecimal amount, String bizType, String bizId, String remark) {
        throw new UnsupportedOperationException("TODO implement consume");
    }
}
