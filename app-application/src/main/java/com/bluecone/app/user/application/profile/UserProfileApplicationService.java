package com.bluecone.app.user.application.profile;

import com.bluecone.app.core.user.domain.repository.UserIdentityRepository;
import com.bluecone.app.core.user.domain.repository.UserProfileRepository;
import com.bluecone.app.user.application.CurrentUserContext;
import com.bluecone.app.user.dto.profile.UpdateUserProfileCommand;
import com.bluecone.app.user.dto.profile.UserProfileDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 用户画像应用服务，负责查询与更新画像。
 */
@Service
@RequiredArgsConstructor
public class UserProfileApplicationService {

    private final UserProfileRepository userProfileRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final CurrentUserContext currentUserContext;

    /**
     * 获取当前用户画像。
     * <p>TODO: 从安全上下文获取 userId，并从领域仓储组装视图。</p>
     */
    public UserProfileDTO getCurrentProfile() {
        Long userId = currentUserContext.getCurrentUserId();
        // TODO: 查询领域模型并转换为 DTO
        return UserProfileDTO.builder().userId(userId).build();
    }

    /**
     * 更新当前用户画像。
     */
    public UserProfileDTO updateCurrentProfile(UpdateUserProfileCommand cmd) {
        Long userId = currentUserContext.getCurrentUserId();
        // TODO: 校验并调用领域聚合更新画像
        return UserProfileDTO.builder().userId(userId).build();
    }
}
