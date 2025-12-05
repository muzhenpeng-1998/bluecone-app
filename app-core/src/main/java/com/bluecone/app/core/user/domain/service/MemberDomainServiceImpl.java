package com.bluecone.app.core.user.domain.service;

import com.bluecone.app.core.user.domain.member.MemberLevel;
import com.bluecone.app.core.user.domain.member.MemberStatus;
import com.bluecone.app.core.user.domain.member.TenantMember;
import com.bluecone.app.core.user.domain.repository.MemberLevelRepository;
import com.bluecone.app.core.user.domain.repository.TenantMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 会员领域服务实现，负责开卡与等级决策。
 */
@Service
@RequiredArgsConstructor
public class MemberDomainServiceImpl implements MemberDomainService {

    private final TenantMemberRepository tenantMemberRepository;
    private final MemberLevelRepository memberLevelRepository;

    @Override
    public TenantMember ensureMemberForUser(Long tenantId, Long userId, String joinChannel) {
        Optional<TenantMember> existing = tenantMemberRepository.findByTenantAndUser(tenantId, userId);
        if (existing.isPresent()) {
            return existing.get();
        }
        TenantMember member = new TenantMember();
        member.setTenantId(tenantId);
        member.setUserId(userId);
        member.setMemberNo(generateMemberNo(tenantId, userId));
        member.setStatus(MemberStatus.ACTIVE);
        member.setJoinChannel(joinChannel);
        member.setJoinAt(LocalDateTime.now());
        member.setGrowthValue(0);
        member.setCreatedAt(LocalDateTime.now());
        member.setUpdatedAt(LocalDateTime.now());
        // TODO: 根据租户策略选择初始等级
        return tenantMemberRepository.save(member);
    }

    @Override
    public MemberLevel determineLevelForGrowth(Long tenantId, int growthValue) {
        List<MemberLevel> levels = memberLevelRepository.findEnabledByTenant(tenantId);
        return levels.stream()
                .filter(l -> l.getMinGrowth() <= growthValue && growthValue <= l.getMaxGrowth())
                .min(Comparator.comparingInt(MemberLevel::getSortOrder))
                .orElse(null);
    }

    private String generateMemberNo(Long tenantId, Long userId) {
        // TODO: 替换为租户可配置的卡号生成器
        return "M" + tenantId + "-" + userId + "-" + UUID.randomUUID().toString().substring(0, 6);
    }
}
