package com.bluecone.app.core.user.domain.member;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 会员关系聚合，连接平台用户与租户，对应表 bc_tenant_member。
 */
@Data
public class TenantMember {

    private Long id;

    private Long tenantId;

    private Long userId;

    private String memberNo;

    private MemberStatus status;

    private String joinChannel;

    private LocalDateTime joinAt;

    private Long levelId;

    private int growthValue;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 开卡入会的工厂方法。
     */
    public static TenantMember enroll(Long tenantId, Long userId, String memberNo, String joinChannel) {
        TenantMember member = new TenantMember();
        member.setTenantId(tenantId);
        member.setUserId(userId);
        member.setMemberNo(memberNo);
        member.setJoinChannel(joinChannel);
        member.setStatus(MemberStatus.ACTIVE);
        member.setJoinAt(LocalDateTime.now());
        member.setGrowthValue(0);
        member.setCreatedAt(LocalDateTime.now());
        member.setUpdatedAt(LocalDateTime.now());
        return member;
    }

    /**
     * 调整会员等级。
     */
    public void changeLevel(Long newLevelId) {
        this.levelId = newLevelId;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 累加成长值。
     */
    public void addGrowth(int delta) {
        // TODO: 根据业务需求决定是否允许负增长
        this.growthValue = this.growthValue + delta;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 增加成长值的语义化方法。
     */
    public void increaseGrowth(int delta) {
        addGrowth(delta);
    }

    /**
     * 冻结会员。
     */
    public void freeze() {
        this.status = MemberStatus.FROZEN;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 会员注销/退出。
     */
    public void quit() {
        this.status = MemberStatus.QUIT;
        this.updatedAt = LocalDateTime.now();
    }
}
