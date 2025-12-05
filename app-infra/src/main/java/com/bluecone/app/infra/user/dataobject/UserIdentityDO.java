package com.bluecone.app.infra.user.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 平台用户身份表映射，表名：bc_user_identity。
 */
@Data
@TableName("bc_user_identity")
public class UserIdentityDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String unionId;

    private String phone;

    private String countryCode;

    private String email;

    private String registerChannel;

    private Integer status;

    private Long firstTenantId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
