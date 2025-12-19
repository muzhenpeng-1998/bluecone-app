package com.bluecone.app.growth.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 邀请码表PO
 */
@Data
@TableName("bc_growth_invite_code")
public class InviteCodePO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;
    
    private Long tenantId;
    
    private String campaignCode;
    
    private String inviteCode;
    
    private Long inviterUserId;
    
    private Integer invitesCount;
    
    private Integer successfulInvitesCount;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
