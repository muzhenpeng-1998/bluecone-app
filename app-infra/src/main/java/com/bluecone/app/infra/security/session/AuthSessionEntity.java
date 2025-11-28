package com.bluecone.app.infra.security.session;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 用户会话持久化实体，对应表 bc_auth_session。
 */
@Data
@TableName("bc_auth_session")
public class AuthSessionEntity implements Serializable {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private Long userId;

    private Long tenantId;

    private String clientType;

    private String deviceId;

    private String refreshTokenHash;

    private LocalDateTime refreshTokenExpireAt;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime lastActiveAt;
}
