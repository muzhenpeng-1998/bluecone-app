package com.bluecone.app.infra.user.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员储值账户表映射，表名：bc_member_balance_account。
 */
@Data
@TableName("bc_member_balance_account")
public class MemberBalanceAccountDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long memberId;

    private BigDecimal availableAmount;

    private BigDecimal frozenAmount;

    private Long version;

    private LocalDateTime updatedAt;

    private LocalDateTime createdAt;
}
