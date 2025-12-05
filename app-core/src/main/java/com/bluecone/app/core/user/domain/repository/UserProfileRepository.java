package com.bluecone.app.core.user.domain.repository;

import java.util.Optional;

import com.bluecone.app.core.user.domain.profile.UserProfile;

/**
 * 用户画像仓储接口。
 */
public interface UserProfileRepository {

    Optional<UserProfile> findByUserId(Long userId);

    UserProfile save(UserProfile profile);
}
