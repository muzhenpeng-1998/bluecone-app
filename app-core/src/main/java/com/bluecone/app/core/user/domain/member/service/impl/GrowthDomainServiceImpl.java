package com.bluecone.app.core.user.domain.member.service.impl;

import com.bluecone.app.core.event.DomainEventPublisher;
import com.bluecone.app.core.user.domain.event.MemberLevelChangedEvent;
import com.bluecone.app.core.user.domain.member.MemberLevel;
import com.bluecone.app.core.user.domain.member.TenantMember;
import com.bluecone.app.core.user.domain.member.service.GrowthDomainService;
import com.bluecone.app.core.user.domain.member.service.MemberLevelPolicy;
import com.bluecone.app.core.user.domain.repository.TenantMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

/**
 * 成长值与等级联动的默认实现。
 */
@Service
@RequiredArgsConstructor
public class GrowthDomainServiceImpl implements GrowthDomainService {

    private final TenantMemberRepository tenantMemberRepository;
    private final MemberLevelPolicy memberLevelPolicy;
    private final DomainEventPublisher domainEventPublisher;

    @Override
    public void increaseGrowthAndCheckLevel(Long tenantId,
                                            Long memberId,
                                            int deltaGrowth,
                                            String bizType,
                                            String bizId) {
        TenantMember member = tenantMemberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("会员不存在: " + memberId));

        // TODO: 确定成长值是否允许为负（扣减场景）以及是否需要流水审计
        member.increaseGrowth(deltaGrowth);

        Optional<MemberLevel> targetLevelOpt = memberLevelPolicy.determineLevel(tenantId, member.getGrowthValue());
        Long oldLevelId = member.getLevelId();
        Long newLevelId = targetLevelOpt.map(MemberLevel::getId).orElse(null);

        if (!Objects.equals(oldLevelId, newLevelId)) {
            member.changeLevel(newLevelId);
            MemberLevelChangedEvent event = new MemberLevelChangedEvent(
                    tenantId,
                    memberId,
                    oldLevelId,
                    newLevelId
            );
            // TODO: 根据事件总线/Outbox 集成具体发布，当前直接发布
            domainEventPublisher.publish(event);
        }

        // TODO: 引入成长值流水表，记录 bizType/bizId
        tenantMemberRepository.save(member);
    }
}
