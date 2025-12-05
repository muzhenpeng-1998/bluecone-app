package com.bluecone.app.infra.user.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会员等级配置表映射，表名：bc_member_level。
 */
@Data
@TableName("bc_member_level")
public class MemberLevelDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String levelCode;

    private String levelName;

    private Integer minGrowth;

    private Integer maxGrowth;

    private String benefitsJson;

    private Integer sortOrder;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
