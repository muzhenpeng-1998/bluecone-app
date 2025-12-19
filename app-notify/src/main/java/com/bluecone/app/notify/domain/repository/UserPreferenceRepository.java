package com.bluecone.app.notify.domain.repository;

import com.bluecone.app.notify.domain.model.UserPreference;

import java.util.Optional;

/**
 * 用户偏好仓储接口
 */
public interface UserPreferenceRepository {
    
    Long save(UserPreference preference);
    
    boolean update(UserPreference preference);
    
    boolean delete(Long userId);
    
    Optional<UserPreference> findByUserId(Long userId, Long tenantId);
}
