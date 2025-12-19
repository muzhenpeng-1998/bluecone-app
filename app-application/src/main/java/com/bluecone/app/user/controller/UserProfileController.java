package com.bluecone.app.user.controller;

import com.bluecone.app.core.api.ApiResponse;
import com.bluecone.app.user.application.profile.UserProfileApplicationService;
import com.bluecone.app.user.dto.profile.UpdateUserProfileCommand;
import com.bluecone.app.user.dto.profile.UserProfileDTO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户画像接口。
 */
@RestController
@RequestMapping("/api/user/profile")
@Validated
public class UserProfileController {

    private final UserProfileApplicationService userProfileApplicationService;

    public UserProfileController(UserProfileApplicationService userProfileApplicationService) {
        this.userProfileApplicationService = userProfileApplicationService;
    }

    /**
     * 获取当前用户画像。
     */
    @GetMapping("/me")
    public ApiResponse<UserProfileDTO> getProfile() {
        UserProfileDTO profile = userProfileApplicationService.getCurrentProfile();
        return ApiResponse.success(profile);
    }

    /**
     * 更新当前用户画像。
     */
    @PutMapping("/me")
    public ApiResponse<UserProfileDTO> updateProfile(@RequestBody UpdateUserProfileCommand command) {
        UserProfileDTO profile = userProfileApplicationService.updateCurrentProfile(command);
        return ApiResponse.success(profile);
    }
}
