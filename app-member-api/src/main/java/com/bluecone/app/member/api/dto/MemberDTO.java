package com.bluecone.app.member.api.dto;

/**
 * 会员信息 DTO
 * 
 * @author bluecone
 * @since 2025-12-18
 */
public class MemberDTO {
    
    /**
     * 会员ID（内部主键）
     */
    private Long memberId;
    
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
     * 会员状态：ACTIVE、INACTIVE、FROZEN
     */
    private String status;
    
    /**
     * 创建时间
     */
    private java.time.LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private java.time.LocalDateTime updatedAt;
    
    // Constructors
    public MemberDTO() {}
    
    public MemberDTO(Long memberId, Long tenantId, Long userId, String memberNo, String status) {
        this.memberId = memberId;
        this.tenantId = tenantId;
        this.userId = userId;
        this.memberNo = memberNo;
        this.status = status;
    }
    
    // Getters and Setters
    public Long getMemberId() {
        return memberId;
    }
    
    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }
    
    public Long getTenantId() {
        return tenantId;
    }
    
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getMemberNo() {
        return memberNo;
    }
    
    public void setMemberNo(String memberNo) {
        this.memberNo = memberNo;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public java.time.LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(java.time.LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public java.time.LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(java.time.LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
