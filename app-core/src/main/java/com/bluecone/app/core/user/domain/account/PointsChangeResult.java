package com.bluecone.app.core.user.domain.account;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 积分变动结果，封装单次增减后的余额等信息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PointsChangeResult {

    /** 本次变动的积分值，可正可负 */
    private int changePoints;

    /** 变动后的可用积分余额 */
    private int balanceAfter;

    /** 是否成功 */
    private boolean success;

    /** 失败原因（如有） */
    private String failReason;

    public static PointsChangeResult success(int delta, int balanceAfter) {
        return new PointsChangeResult(delta, balanceAfter, true, null);
    }

    public static PointsChangeResult idempotent(int delta, int balanceAfter) {
        return new PointsChangeResult(delta, balanceAfter, true, "IDEMPOTENT");
    }

    public static PointsChangeResult failed(String reason) {
        return new PointsChangeResult(0, 0, false, reason);
    }
}
