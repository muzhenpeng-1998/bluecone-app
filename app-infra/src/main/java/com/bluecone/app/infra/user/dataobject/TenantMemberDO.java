package com.bluecone.app.infra.user.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户会员关系表映射，表名：bc_tenant_member。
 */
@Data
@TableName("bc_tenant_member")
public class TenantMemberDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long userId;

    private String memberNo;

    private Integer status;

    private String joinChannel;

    private LocalDateTime joinAt;

    private Long levelId;

    private Integer growthValue;

    private String remark;

    private String extraJson;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
