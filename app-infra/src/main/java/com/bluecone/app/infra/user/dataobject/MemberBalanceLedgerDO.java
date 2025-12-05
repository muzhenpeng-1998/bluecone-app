package com.bluecone.app.infra.user.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 储值流水表映射，表名：bc_member_balance_ledger。
 */
@Data
@TableName("bc_member_balance_ledger")
public class MemberBalanceLedgerDO {

    @TableId(type = IdType.AUTO)
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
