package com.bluecone.app.infra.user.runtime;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.core.contextkit.SnapshotLoadKey;
import com.bluecone.app.core.user.domain.member.MemberStatus;
import com.bluecone.app.core.user.runtime.api.UserSnapshot;
import com.bluecone.app.core.user.runtime.spi.UserSnapshotRepository;
import com.bluecone.app.id.core.Ulid128;
import com.bluecone.app.infra.user.dataobject.TenantMemberDO;
import com.bluecone.app.infra.user.dataobject.UserIdentityDO;
import com.bluecone.app.infra.user.dataobject.UserProfileDO;
import com.bluecone.app.infra.user.mapper.TenantMemberMapper;
import com.bluecone.app.infra.user.mapper.UserIdentityMapper;
import com.bluecone.app.infra.user.mapper.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.ZoneOffset;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 基于用户身份 + 会员关系 + 画像的 UserSnapshot 仓储实现。
 */
@Repository
@RequiredArgsConstructor
public class UserSnapshotRepositoryImpl implements UserSnapshotRepository {

    private final UserIdentityMapper userIdentityMapper;
    private final TenantMemberMapper tenantMemberMapper;
    private final UserProfileMapper userProfileMapper;

    @Override
    public Optional<UserSnapshot> loadFull(SnapshotLoadKey key) {
        Ulid128 internalId = (Ulid128) key.scopeId();
        // 当前内部仍以 Long userId 为主键，这里简单将 internalId.lsb 映射为 userId（后续可接入真正的 internal_id）
        Long userId = internalId != null ? internalId.lsb() : null;
        if (userId == null) {
            return Optional.empty();
        }

        UserIdentityDO identity = userIdentityMapper.selectById(userId);
        if (identity == null) {
            return Optional.empty();
        }

        TenantMemberDO member = tenantMemberMapper.selectOne(new LambdaQueryWrapper<TenantMemberDO>()
                .eq(TenantMemberDO::getTenantId, key.tenantId())
                .eq(TenantMemberDO::getUserId, userId));
        UserProfileDO profile = userProfileMapper.selectById(userId);

        int status = identity.getStatus() != null ? identity.getStatus() : 1;
        boolean phoneBound = identity.getPhone() != null && !identity.getPhone().isEmpty();
        String memberLevel = null;
        if (member != null && member.getLevelId() != null) {
            memberLevel = String.valueOf(member.getLevelId());
        }

        long configVersion = deriveVersion(identity, member, profile);
        Instant updatedAt = mostRecentUpdatedAt(identity, member, profile);

        Map<String, Object> ext = new HashMap<>();
        if (member != null) {
            ext.put("memberStatus", member.getStatus());
        }
        if (profile != null) {
            ext.put("nickname", profile.getNickname());
        }
        if (identity.getId() != null) {
            ext.put("numericUserId", identity.getId());
        }

        UserSnapshot snapshot = new UserSnapshot(
                key.tenantId(),
                internalId,
                status,
                phoneBound,
                memberLevel,
                configVersion,
                updatedAt,
                ext
        );
        return Optional.of(snapshot);
    }

    @Override
    public Optional<Long> loadVersion(SnapshotLoadKey key) {
        Ulid128 internalId = (Ulid128) key.scopeId();
        Long userId = internalId != null ? internalId.lsb() : null;
        if (userId == null) {
            return Optional.empty();
        }
        UserIdentityDO identity = userIdentityMapper.selectById(userId);
        if (identity == null || identity.getUpdatedAt() == null) {
            return Optional.empty();
        }
        return Optional.of(identity.getUpdatedAt().toInstant(ZoneOffset.UTC).toEpochMilli());
    }

    private long deriveVersion(UserIdentityDO identity,
                               TenantMemberDO member,
                               UserProfileDO profile) {
        Instant max = mostRecentUpdatedAt(identity, member, profile);
        return max != null ? max.toEpochMilli() : 0L;
    }

    private Instant mostRecentUpdatedAt(UserIdentityDO identity,
                                        TenantMemberDO member,
                                        UserProfileDO profile) {
        Instant max = null;
        if (identity != null && identity.getUpdatedAt() != null) {
            max = identity.getUpdatedAt().toInstant(ZoneOffset.UTC);
        }
        if (member != null && member.getUpdatedAt() != null) {
            Instant v = member.getUpdatedAt().toInstant(ZoneOffset.UTC);
            if (max == null || v.isAfter(max)) {
                max = v;
            }
        }
        if (profile != null && profile.getUpdatedAt() != null) {
            Instant v = profile.getUpdatedAt().toInstant(ZoneOffset.UTC);
            if (max == null || v.isAfter(max)) {
                max = v;
            }
        }
        return max;
    }
}
