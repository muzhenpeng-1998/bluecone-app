package com.bluecone.app.infra.user.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 积分流水表映射，表名：bc_member_points_ledger。
 */
@Data
@TableName("bc_member_points_ledger")
public class MemberPointsLedgerDO {

    @TableId(type = IdType.AUTO)
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
