package com.bluecone.app.infra.user.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户第三方账号绑定表映射，表名：bc_user_third_auth。
 */
@Data
@TableName("bc_user_third_auth")
public class UserThirdAuthDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String authType;

    private String appId;

    private String openId;

    private String unionId;

    private String extraJson;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
