package com.bluecone.app.notify.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.notify.domain.model.UserPreference;
import com.bluecone.app.notify.domain.repository.UserPreferenceRepository;
import com.bluecone.app.notify.infrastructure.converter.NotifyConverter;
import com.bluecone.app.notify.infrastructure.dao.UserPreferenceDO;
import com.bluecone.app.notify.infrastructure.dao.UserPreferenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户偏好仓储实现
 */
@Repository
@RequiredArgsConstructor
public class UserPreferenceRepositoryImpl implements UserPreferenceRepository {
    
    private final UserPreferenceMapper mapper;
    
    @Override
    public Long save(UserPreference preference) {
        UserPreferenceDO dataObject = NotifyConverter.toDO(preference);
        mapper.insert(dataObject);
        return dataObject.getId();
    }
    
    @Override
    public boolean update(UserPreference preference) {
        UserPreferenceDO dataObject = NotifyConverter.toDO(preference);
        return mapper.updateById(dataObject) > 0;
    }
    
    @Override
    public boolean delete(Long userId) {
        LambdaQueryWrapper<UserPreferenceDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPreferenceDO::getUserId, userId);
        return mapper.delete(wrapper) > 0;
    }
    
    @Override
    public Optional<UserPreference> findByUserId(Long userId, Long tenantId) {
        LambdaQueryWrapper<UserPreferenceDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPreferenceDO::getUserId, userId);
        if (tenantId != null) {
            wrapper.eq(UserPreferenceDO::getTenantId, tenantId);
        }
        UserPreferenceDO dataObject = mapper.selectOne(wrapper);
        return Optional.ofNullable(NotifyConverter.toDomain(dataObject));
    }
}
