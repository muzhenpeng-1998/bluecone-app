package com.bluecone.app.user.domain;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 用户基础实体，对应表 bc_user。
 *
 * 仅保留认证所需字段，后续可扩展手机号、邮箱、头像等。
 */
@Data
@TableName("bc_user")
public class UserEntity implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String username;

    /**
     * 密码散列（当前为简化实现，可替换为 BCrypt 等）。
     */
    private String passwordHash;

    /**
     * 用户状态：ACTIVE/LOCKED/DISABLED 等，暂不做校验。
     */
    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
