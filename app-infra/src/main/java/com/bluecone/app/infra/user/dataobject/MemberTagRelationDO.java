package com.bluecone.app.infra.user.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会员与标签关联表映射，表名：bc_member_tag_relation。
 */
@Data
@TableName("bc_member_tag_relation")
public class MemberTagRelationDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long memberId;

    private Long tagId;

    private LocalDateTime createdAt;
}
