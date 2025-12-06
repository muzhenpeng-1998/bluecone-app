package com.bluecone.app.core.user.domain.member.service;

import com.bluecone.app.core.user.domain.member.MemberLevel;
import com.bluecone.app.core.user.domain.repository.MemberLevelRepository;
import com.bluecone.app.core.user.domain.member.service.impl.MemberLevelPolicyImpl;
import com.bluecone.app.core.user.testdata.UserTestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class MemberLevelPolicyImplTest {

    private MemberLevelRepository memberLevelRepository;
    private MemberLevelPolicy policy;

    @BeforeEach
    void setUp() {
        memberLevelRepository = Mockito.mock(MemberLevelRepository.class);
        policy = new MemberLevelPolicyImpl(memberLevelRepository);
        List<MemberLevel> levels = List.of(
                UserTestDataFactory.aMemberLevel(1L, "L1", 0, 100),
                UserTestDataFactory.aMemberLevel(2L, "L2", 101, 200),
                UserTestDataFactory.aMemberLevel(3L, "L3", 201, 500)
        );
        when(memberLevelRepository.findEnabledByTenant(100L)).thenReturn(levels);
    }

    @Test
    void shouldReturnMatchedLevel() {
        Optional<MemberLevel> level = policy.determineLevel(100L, 150);
        assertThat(level).isPresent();
        assertThat(level.get().getId()).isEqualTo(2L);
    }

    @Test
    void shouldReturnEmptyWhenNoMatch() {
        Optional<MemberLevel> level = policy.determineLevel(100L, 999);
        assertThat(level).isEmpty();
    }
}
