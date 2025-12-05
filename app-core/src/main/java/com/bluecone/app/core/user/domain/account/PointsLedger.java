package com.bluecone.app.core.user.domain.account;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 积分流水领域对象，对应表 bc_member_points_ledger。
 */
@Data
public class PointsLedger {

    private Long id;

    private Long tenantId;

    private Long memberId;

    private String bizType;

    private String bizId;

    private Integer changePoints;

    private Integer balanceAfter;

    private String remark;

    private LocalDateTime occurredAt;

    private LocalDateTime createdAt;
}
