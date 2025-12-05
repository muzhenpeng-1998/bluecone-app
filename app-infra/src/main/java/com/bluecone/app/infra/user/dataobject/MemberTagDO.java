package com.bluecone.app.infra.user.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会员标签定义表映射，表名：bc_member_tag。
 */
@Data
@TableName("bc_member_tag")
public class MemberTagDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String tagCode;

    private String tagName;

    private String color;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
