package com.bluecone.app.member.domain.model;

import com.bluecone.app.member.domain.enums.MemberStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会员聚合根
 * 
 * @author bluecone
 * @since 2025-12-18
 */
@Data
public class Member {
    
    /**
     * 会员ID（内部主键）
     */
    private Long id;
    
    /**
     * 租户ID
     */
    private Long tenantId;
    
    /**
     * 平台用户ID
     */
    private Long userId;
    
    /**
     * 会员编号（租户内唯一，对外展示）
     */
    private String memberNo;
    
    /**
     * 会员状态
     */
    private MemberStatus status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 创建人
     */
    private Long createdBy;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 更新人
     */
    private Long updatedBy;
    
    /**
     * 创建新会员
     */
    public static Member create(Long id, Long tenantId, Long userId, String memberNo) {
        Member member = new Member();
        member.id = id;
        member.tenantId = tenantId;
        member.userId = userId;
        member.memberNo = memberNo;
        member.status = MemberStatus.ACTIVE;
        member.createdAt = LocalDateTime.now();
        member.updatedAt = LocalDateTime.now();
        return member;
    }
    
    /**
     * 判断会员是否可用
     */
    public boolean isActive() {
        return status == MemberStatus.ACTIVE;
    }
    
    /**
     * 冻结会员
     */
    public void freeze() {
        this.status = MemberStatus.FROZEN;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 激活会员
     */
    public void activate() {
        this.status = MemberStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 停用会员
     */
    public void deactivate() {
        this.status = MemberStatus.INACTIVE;
        this.updatedAt = LocalDateTime.now();
    }
}
