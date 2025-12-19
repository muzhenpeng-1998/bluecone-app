package com.bluecone.app.member.api.dto;

/**
 * 积分余额 DTO
 * 
 * @author bluecone
 * @since 2025-12-18
 */
public class PointsBalanceDTO {
    
    /**
     * 会员ID
     */
    private Long memberId;
    
    /**
     * 租户ID
     */
    private Long tenantId;
    
    /**
     * 可用积分
     */
    private Long availablePoints;
    
    /**
     * 冻结积分
     */
    private Long frozenPoints;
    
    /**
     * 总积分（可用+冻结）
     */
    private Long totalPoints;
    
    // Constructors
    public PointsBalanceDTO() {}
    
    public PointsBalanceDTO(Long memberId, Long tenantId, Long availablePoints, Long frozenPoints) {
        this.memberId = memberId;
        this.tenantId = tenantId;
        this.availablePoints = availablePoints;
        this.frozenPoints = frozenPoints;
        this.totalPoints = availablePoints + frozenPoints;
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
    
    public Long getAvailablePoints() {
        return availablePoints;
    }
    
    public void setAvailablePoints(Long availablePoints) {
        this.availablePoints = availablePoints;
        this.totalPoints = this.availablePoints + (this.frozenPoints != null ? this.frozenPoints : 0);
    }
    
    public Long getFrozenPoints() {
        return frozenPoints;
    }
    
    public void setFrozenPoints(Long frozenPoints) {
        this.frozenPoints = frozenPoints;
        this.totalPoints = (this.availablePoints != null ? this.availablePoints : 0) + this.frozenPoints;
    }
    
    public Long getTotalPoints() {
        return totalPoints;
    }
    
    public void setTotalPoints(Long totalPoints) {
        this.totalPoints = totalPoints;
    }
}
