package com.bluecone.app.user.controller;

import com.bluecone.app.core.api.ApiResponse;
import com.bluecone.app.user.application.auth.UserAuthApplicationService;
import com.bluecone.app.user.dto.auth.WechatMiniAppLoginRequest;
import com.bluecone.app.user.dto.auth.LoginResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户认证相关接口。
 */
@RestController
@RequestMapping("/api/user/auth")
@Validated
public class UserAuthController {

    private final UserAuthApplicationService userAuthApplicationService;

    public UserAuthController(UserAuthApplicationService userAuthApplicationService) {
        this.userAuthApplicationService = userAuthApplicationService;
    }

    /**
     * 微信小程序登录。
     */
    @PostMapping("/wechat-miniapp/login")
    public ApiResponse<LoginResponse> loginByWeChatMiniApp(@RequestBody WechatMiniAppLoginRequest command) {
        LoginResponse result = userAuthApplicationService.loginByWeChatMiniApp(command);
        return ApiResponse.success(result);
    }
}
