package com.bluecone.app.core.user.domain.member;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TenantMemberTest {

    @Test
    void enrollShouldInitFields() {
        TenantMember member = TenantMember.enroll(100L, 1L, "NO1", "JOIN");

        assertThat(member.getTenantId()).isEqualTo(100L);
        assertThat(member.getUserId()).isEqualTo(1L);
        assertThat(member.getMemberNo()).isEqualTo("NO1");
        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(member.getJoinChannel()).isEqualTo("JOIN");
        assertThat(member.getGrowthValue()).isZero();
        assertThat(member.getJoinAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void increaseGrowthShouldAccumulate() {
        TenantMember member = new TenantMember();
        member.setGrowthValue(10);
        member.increaseGrowth(5);
        assertThat(member.getGrowthValue()).isEqualTo(15);
    }

    @Test
    void changeLevelShouldUpdateLevelId() {
        TenantMember member = new TenantMember();
        member.changeLevel(2L);
        assertThat(member.getLevelId()).isEqualTo(2L);
    }

    @Test
    void freezeAndQuitShouldChangeStatus() {
        TenantMember member = new TenantMember();
        member.freeze();
        assertThat(member.getStatus()).isEqualTo(MemberStatus.FROZEN);

        member.quit();
        assertThat(member.getStatus()).isEqualTo(MemberStatus.QUIT);
    }
}
