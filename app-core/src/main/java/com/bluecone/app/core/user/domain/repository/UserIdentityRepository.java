package com.bluecone.app.core.user.domain.repository;

import java.util.Optional;

import com.bluecone.app.core.user.domain.identity.UserIdentity;

/**
 * 用户身份仓储接口，屏蔽 ORM 细节。
 */
public interface UserIdentityRepository {

    Optional<UserIdentity> findById(Long id);

    Optional<UserIdentity> findByUnionId(String unionId);

    Optional<UserIdentity> findByPhone(String countryCode, String phone);

    UserIdentity save(UserIdentity identity);
}
