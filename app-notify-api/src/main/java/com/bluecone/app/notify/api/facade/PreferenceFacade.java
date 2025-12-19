package com.bluecone.app.notify.api.facade;

import com.bluecone.app.notify.api.dto.UpdateUserPreferenceRequest;
import com.bluecone.app.notify.api.dto.UserPreferenceDTO;

/**
 * 用户偏好管理门面
 * 管理用户通知偏好与免打扰设置
 */
public interface PreferenceFacade {
    
    /**
     * 更新用户偏好
     * 
     * @param userId 用户ID
     * @param tenantId 租户ID（可选）
     * @param request 更新请求
     * @return 是否更新成功
     */
    boolean updateUserPreference(Long userId, Long tenantId, UpdateUserPreferenceRequest request);
    
    /**
     * 查询用户偏好
     * 
     * @param userId 用户ID
     * @param tenantId 租户ID（可选）
     * @return 用户偏好
     */
    UserPreferenceDTO getUserPreference(Long userId, Long tenantId);
    
    /**
     * 重置用户偏好为默认值
     * 
     * @param userId 用户ID
     * @param tenantId 租户ID（可选）
     * @return 是否重置成功
     */
    boolean resetUserPreference(Long userId, Long tenantId);
}
