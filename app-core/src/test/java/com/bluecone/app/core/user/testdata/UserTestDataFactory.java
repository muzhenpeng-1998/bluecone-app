package com.bluecone.app.core.user.testdata;

import com.bluecone.app.core.user.domain.identity.RegisterChannel;
import com.bluecone.app.core.user.domain.identity.UserIdentity;
import com.bluecone.app.core.user.domain.identity.UserStatus;
import com.bluecone.app.core.user.domain.member.MemberLevel;
import com.bluecone.app.core.user.domain.member.MemberStatus;
import com.bluecone.app.core.user.domain.member.TenantMember;

import java.time.LocalDateTime;

/**
 * 测试数据工厂，构造常用领域对象。
 */
public final class UserTestDataFactory {

    private UserTestDataFactory() {
    }

    public static UserIdentity aUserIdentity() {
        UserIdentity identity = new UserIdentity();
        identity.setId(1L);
        identity.setPhone("13800000000");
        identity.setCountryCode("+86");
        identity.setRegisterChannel(RegisterChannel.WECHAT_MINI);
        identity.setStatus(UserStatus.ACTIVE);
        identity.setFirstTenantId(100L);
        identity.setCreatedAt(LocalDateTime.now());
        identity.setUpdatedAt(LocalDateTime.now());
        return identity;
    }

    public static TenantMember aTenantMember() {
        TenantMember member = new TenantMember();
        member.setId(10L);
        member.setTenantId(100L);
        member.setUserId(1L);
        member.setMemberNo("M100-1");
        member.setStatus(MemberStatus.ACTIVE);
        member.setJoinChannel("TEST");
        member.setJoinAt(LocalDateTime.now().minusDays(1));
        member.setGrowthValue(0);
        member.setCreatedAt(LocalDateTime.now().minusDays(1));
        member.setUpdatedAt(LocalDateTime.now().minusDays(1));
        return member;
    }

    public static MemberLevel aMemberLevel(Long id, String code, int minGrowth, int maxGrowth) {
        MemberLevel level = new MemberLevel();
        level.setId(id);
        level.setTenantId(100L);
        level.setLevelCode(code);
        level.setLevelName("Level-" + code);
        level.setMinGrowth(minGrowth);
        level.setMaxGrowth(maxGrowth);
        level.setSortOrder(minGrowth);
        level.setStatus(1);
        level.setCreatedAt(LocalDateTime.now().minusDays(1));
        level.setUpdatedAt(LocalDateTime.now().minusDays(1));
        return level;
    }
}
