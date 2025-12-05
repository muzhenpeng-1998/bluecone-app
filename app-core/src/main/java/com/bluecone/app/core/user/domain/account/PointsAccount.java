package com.bluecone.app.core.user.domain.account;

import lombok.Data;

/**
 * 会员积分账户聚合，对应表 bc_member_points_account。
 */
@Data
public class PointsAccount {

    private Long id;

    private Long tenantId;

    private Long memberId;

    private int pointsBalance;

    private int frozenPoints;

    private long version;

    /**
     * 初始化空账户。
     */
    public static PointsAccount initFor(Long tenantId, Long memberId) {
        PointsAccount account = new PointsAccount();
        account.setTenantId(tenantId);
        account.setMemberId(memberId);
        account.setPointsBalance(0);
        account.setFrozenPoints(0);
        account.setVersion(0);
        return account;
    }

    /**
     * 应用积分变更并递增版本。
     */
    public void applyChange(int delta) {
        this.pointsBalance = this.pointsBalance + delta;
        this.version = this.version + 1;
    }

    /**
     * 积分获取。
     */
    public PointsChangeResult earn(int points, String bizType, String bizId, String remark) {
        throw new UnsupportedOperationException("TODO implement earn");
    }

    /**
     * 积分消耗。
     */
    public PointsChangeResult consume(int points, String bizType, String bizId, String remark) {
        throw new UnsupportedOperationException("TODO implement consume");
    }
}
