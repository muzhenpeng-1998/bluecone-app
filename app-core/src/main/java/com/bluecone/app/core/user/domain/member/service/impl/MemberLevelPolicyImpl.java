package com.bluecone.app.core.user.domain.member.service.impl;

import com.bluecone.app.core.user.domain.member.MemberLevel;
import com.bluecone.app.core.user.domain.member.service.MemberLevelPolicy;
import com.bluecone.app.core.user.domain.repository.MemberLevelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 默认等级策略：按成长值落在哪个区间决定等级。
 */
@Service
@RequiredArgsConstructor
public class MemberLevelPolicyImpl implements MemberLevelPolicy {

    private final MemberLevelRepository memberLevelRepository;

    @Override
    public Optional<MemberLevel> determineLevel(Long tenantId, int growthValue) {
        List<MemberLevel> levels = memberLevelRepository.findEnabledByTenant(tenantId);
        for (MemberLevel level : levels) {
            if (level.matchesGrowth(growthValue)) {
                return Optional.of(level);
            }
        }
        // TODO: 可扩展“只升不降”等策略
        return Optional.empty();
    }
}
