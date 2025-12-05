package com.bluecone.app.core.user.domain.member.service;

import com.bluecone.app.core.user.domain.member.MemberLevel;

import java.util.Optional;

/**
 * 会员等级策略，根据成长值确定等级。
 */
public interface MemberLevelPolicy {

    Optional<MemberLevel> determineLevel(Long tenantId, int growthValue);
}
