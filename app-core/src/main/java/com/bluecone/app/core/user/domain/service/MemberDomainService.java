package com.bluecone.app.core.user.domain.service;

import com.bluecone.app.core.user.domain.member.MemberLevel;
import com.bluecone.app.core.user.domain.member.TenantMember;

/**
 * 会员领域服务，封装开卡与等级决策。
 */
public interface MemberDomainService {

    /**
     * 确保租户下存在会员，不存在则自动开卡。
     */
    TenantMember ensureMemberForUser(Long tenantId, Long userId, String joinChannel);

    /**
     * 根据成长值决策会员等级（预留）。
     */
    MemberLevel determineLevelForGrowth(Long tenantId, int growthValue);
}
