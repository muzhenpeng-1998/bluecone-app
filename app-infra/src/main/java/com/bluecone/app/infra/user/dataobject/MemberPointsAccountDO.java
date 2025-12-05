package com.bluecone.app.infra.user.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会员积分账户表映射，表名：bc_member_points_account。
 */
@Data
@TableName("bc_member_points_account")
public class MemberPointsAccountDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long memberId;

    private Integer pointsBalance;

    private Integer frozenPoints;

    private Long version;

    private LocalDateTime updatedAt;

    private LocalDateTime createdAt;
}
