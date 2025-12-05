package com.bluecone.app.infra.user.repositoryimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.core.user.domain.identity.RegisterChannel;
import com.bluecone.app.core.user.domain.identity.UserIdentity;
import com.bluecone.app.core.user.domain.identity.UserStatus;
import com.bluecone.app.core.user.domain.repository.UserIdentityRepository;
import com.bluecone.app.infra.user.dataobject.UserIdentityDO;
import com.bluecone.app.infra.user.mapper.UserIdentityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户身份仓储实现，基于 MyBatis-Plus 访问表 bc_user_identity。
 */
@Repository
@RequiredArgsConstructor
public class UserIdentityRepositoryImpl implements UserIdentityRepository {

    private final UserIdentityMapper userIdentityMapper;

    @Override
    public Optional<UserIdentity> findById(Long id) {
        return Optional.ofNullable(toDomain(userIdentityMapper.selectById(id)));
    }

    @Override
    public Optional<UserIdentity> findByUnionId(String unionId) {
        UserIdentityDO dataObject = userIdentityMapper.selectOne(new LambdaQueryWrapper<UserIdentityDO>()
                .eq(UserIdentityDO::getUnionId, unionId));
        return Optional.ofNullable(toDomain(dataObject));
    }

    @Override
    public Optional<UserIdentity> findByPhone(String countryCode, String phone) {
        UserIdentityDO dataObject = userIdentityMapper.selectOne(new LambdaQueryWrapper<UserIdentityDO>()
                .eq(UserIdentityDO::getCountryCode, countryCode)
                .eq(UserIdentityDO::getPhone, phone));
        return Optional.ofNullable(toDomain(dataObject));
    }

    @Override
    public UserIdentity save(UserIdentity identity) {
        UserIdentityDO dataObject = toDO(identity);
        if (identity.getId() == null) {
            userIdentityMapper.insert(dataObject);
            identity.setId(dataObject.getId());
        } else {
            userIdentityMapper.updateById(dataObject);
        }
        return identity;
    }

    private UserIdentity toDomain(UserIdentityDO dataObject) {
        if (dataObject == null) {
            return null;
        }
        UserIdentity identity = new UserIdentity();
        identity.setId(dataObject.getId());
        identity.setUnionId(dataObject.getUnionId());
        identity.setPhone(dataObject.getPhone());
        identity.setCountryCode(dataObject.getCountryCode());
        identity.setEmail(dataObject.getEmail());
        identity.setRegisterChannel(parseRegisterChannel(dataObject.getRegisterChannel()));
        identity.setStatus(parseUserStatus(dataObject.getStatus()));
        identity.setFirstTenantId(dataObject.getFirstTenantId());
        identity.setCreatedAt(dataObject.getCreatedAt());
        identity.setUpdatedAt(dataObject.getUpdatedAt());
        return identity;
    }

    private UserIdentityDO toDO(UserIdentity identity) {
        if (identity == null) {
            return null;
        }
        UserIdentityDO dataObject = new UserIdentityDO();
        dataObject.setId(identity.getId());
        dataObject.setUnionId(identity.getUnionId());
        dataObject.setPhone(identity.getPhone());
        dataObject.setCountryCode(identity.getCountryCode());
        dataObject.setEmail(identity.getEmail());
        dataObject.setRegisterChannel(identity.getRegisterChannel() != null ? identity.getRegisterChannel().name() : null);
        dataObject.setStatus(toStatusValue(identity.getStatus()));
        dataObject.setFirstTenantId(identity.getFirstTenantId());
        dataObject.setCreatedAt(identity.getCreatedAt());
        dataObject.setUpdatedAt(identity.getUpdatedAt());
        return dataObject;
    }

    private RegisterChannel parseRegisterChannel(String value) {
        return value == null ? null : RegisterChannel.valueOf(value);
    }

    private UserStatus parseUserStatus(Integer status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case 1 -> UserStatus.ACTIVE;
            case 0 -> UserStatus.FROZEN;
            case -1 -> UserStatus.DELETED;
            default -> null;
        };
    }

    private Integer toStatusValue(UserStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case ACTIVE -> 1;
            case FROZEN -> 0;
            case DELETED -> -1;
        };
    }
}
