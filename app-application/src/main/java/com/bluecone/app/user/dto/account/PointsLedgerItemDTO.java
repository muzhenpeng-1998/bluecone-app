package com.bluecone.app.user.dto.account;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 积分流水条目 DTO，对应 bc_member_points_ledger。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointsLedgerItemDTO {

    private Long id;

    private String bizType;

    private String bizId;

    private Integer changePoints;

    private Integer balanceAfter;

    private String remark;

    private String occurredAt;
}
