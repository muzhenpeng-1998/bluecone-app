package com.bluecone.app.core.user.domain.member.service;

import com.bluecone.app.core.event.DomainEventPublisher;
import com.bluecone.app.core.user.domain.event.MemberLevelChangedEvent;
import com.bluecone.app.core.user.domain.member.MemberLevel;
import com.bluecone.app.core.user.domain.member.TenantMember;
import com.bluecone.app.core.user.domain.repository.TenantMemberRepository;
import com.bluecone.app.core.user.domain.member.service.impl.GrowthDomainServiceImpl;
import com.bluecone.app.core.user.testdata.UserTestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GrowthDomainServiceImplTest {

    @Mock
    private TenantMemberRepository tenantMemberRepository;
    @Mock
    private MemberLevelPolicy memberLevelPolicy;
    @Mock
    private DomainEventPublisher domainEventPublisher;

    private GrowthDomainServiceImpl growthDomainService;

    @BeforeEach
    void setUp() {
        growthDomainService = new GrowthDomainServiceImpl(tenantMemberRepository, memberLevelPolicy, domainEventPublisher);
    }

    @Test
    void shouldPublishEventWhenLevelChanged() {
        TenantMember member = UserTestDataFactory.aTenantMember();
        member.setLevelId(1L);
        when(tenantMemberRepository.findById(10L)).thenReturn(Optional.of(member));
        MemberLevel newLevel = UserTestDataFactory.aMemberLevel(2L, "L2", 10, 100);
        when(memberLevelPolicy.determineLevel(100L, member.getGrowthValue() + 10)).thenReturn(Optional.of(newLevel));

        growthDomainService.increaseGrowthAndCheckLevel(100L, 10L, 10, "ORDER", "BIZ1");

        verify(tenantMemberRepository).save(member);
        ArgumentCaptor<MemberLevelChangedEvent> captor = ArgumentCaptor.forClass(MemberLevelChangedEvent.class);
        verify(domainEventPublisher).publish(captor.capture());
        assertThat(captor.getValue().getNewLevelId()).isEqualTo(2L);
    }

    @Test
    void shouldNotPublishEventWhenLevelUnchanged() {
        TenantMember member = UserTestDataFactory.aTenantMember();
        member.setLevelId(null);
        when(tenantMemberRepository.findById(10L)).thenReturn(Optional.of(member));
        when(memberLevelPolicy.determineLevel(any(), anyInt())).thenReturn(Optional.empty());

        growthDomainService.increaseGrowthAndCheckLevel(100L, 10L, 5, "ORDER", "BIZ1");

        verify(domainEventPublisher, never()).publish(any());
        verify(tenantMemberRepository).save(member);
    }
}
