package com.bluecone.app.user.application.auth;

import com.bluecone.app.core.event.DomainEventPublisher;
import com.bluecone.app.core.user.domain.event.UserRegisteredEvent;
import com.bluecone.app.core.user.domain.identity.RegisterChannel;
import com.bluecone.app.core.user.domain.identity.UserIdentity;
import com.bluecone.app.core.user.domain.member.TenantMember;
import com.bluecone.app.core.user.domain.service.MemberDomainService;
import com.bluecone.app.core.user.domain.service.UserDomainService;
import com.bluecone.app.core.user.domain.service.UserDomainService.UserRegistrationResult;
import com.bluecone.app.infra.wechat.WeChatCode2SessionResult;
import com.bluecone.app.infra.wechat.WeChatMiniAppClient;
import com.bluecone.app.infra.wechat.WeChatPhoneNumberResult;
import com.bluecone.app.security.session.AuthSessionCreateResult;
import com.bluecone.app.security.session.AuthSessionManager;
import com.bluecone.app.user.dto.auth.WechatMiniAppLoginRequest;
import com.bluecone.app.user.dto.auth.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 用户认证应用服务，负责任务编排与跨聚合协调。
 */
@Service
@RequiredArgsConstructor
public class UserAuthApplicationService {

    private final WeChatMiniAppClient weChatMiniAppClient;
    private final DomainEventPublisher domainEventPublisher;
    private final UserDomainService userDomainService;
    private final MemberDomainService memberDomainService;
    private final AuthSessionManager authSessionManager;

    /**
     * 微信小程序登录/注册。
     */
    public LoginResponse loginByWeChatMiniApp(WechatMiniAppLoginRequest cmd) {
        // TODO: 从配置加载 appId，与租户或渠道绑定
        String appId = "WECHAT_MINI_APP_ID_PLACEHOLDER";

        // 1. code 换 session
        WeChatCode2SessionResult sessionResult = weChatMiniAppClient.code2Session(appId, cmd.getCode());

        // 2. 解密手机号（可选）
        String phone = null;
        String countryCode = "+86";
        if (StringUtils.hasText(cmd.getEncryptedData()) && StringUtils.hasText(cmd.getIv())) {
            WeChatPhoneNumberResult phoneResult = weChatMiniAppClient.decryptPhoneNumber(appId, sessionResult.getSessionKey(), cmd.getEncryptedData(), cmd.getIv());
            if (phoneResult != null) {
                phone = phoneResult.getPhoneNumber();
                countryCode = phoneResult.getCountryCode();
            }
        }

        // 3. 注册或加载用户
        UserRegistrationResult registerResult = userDomainService.registerOrLoadByWeChatUnionId(
                sessionResult.getUnionId(),
                phone,
                countryCode,
                cmd.getSourceTenantId(),
                RegisterChannel.WECHAT_MINI
        );
        UserIdentity identity = registerResult.identity();

        if (registerResult.isNew()) {
            UserRegisteredEvent event = new UserRegisteredEvent(
                    identity.getId(),
                    identity.getFirstTenantId(),
                    identity.getUnionId(),
                    identity.getPhone(),
                    identity.getRegisterChannel() != null ? identity.getRegisterChannel().name() : null
            );
            domainEventPublisher.publish(event);
        }

        // 4. TODO: 更新画像（昵称、头像等）

        // 5. 选择租户
        Long tenantId = cmd.getSourceTenantId() != null ? cmd.getSourceTenantId() : identity.getFirstTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("登录缺少租户信息");
        }

        // 6. 确保会员存在
        TenantMember member = memberDomainService.ensureMemberForUser(tenantId, identity.getId(), "JOIN_WECHAT_AUTO");

        // 7. 创建会话
        AuthSessionCreateResult session = authSessionManager.createSession(identity.getId(), tenantId, "MINIAPP", null, null, null);

        // 8. 组装结果
        LoginResponse result = new LoginResponse();
        result.setAccessToken(session.getAccessToken());
        result.setRefreshToken(session.getRefreshToken());
        result.setExpireAt(session.getExpireAt());
        result.setUserId(identity.getId());
        result.setTenantId(tenantId);
        result.setMemberId(member.getId());
        result.setNewUser(null); // TODO: 标记是否新注册
        result.setNewMember(null); // TODO: 标记是否新会员
        return result;
    }
}
