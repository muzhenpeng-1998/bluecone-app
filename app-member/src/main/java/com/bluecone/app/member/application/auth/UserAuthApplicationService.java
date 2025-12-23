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
import com.bluecone.app.infra.wechat.openplatform.WechatAuthorizedAppService;
import com.bluecone.app.security.session.AuthSessionCreateResult;
import com.bluecone.app.security.session.AuthSessionManager;
import com.bluecone.app.user.dto.auth.WechatMiniAppLoginRequest;
import com.bluecone.app.user.dto.auth.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 用户认证应用服务，负责任务编排与跨聚合协调。
 */
@Service
@RequiredArgsConstructor
public class UserAuthApplicationService {

    private static final Logger log = LoggerFactory.getLogger(UserAuthApplicationService.class);

    private final WeChatMiniAppClient weChatMiniAppClient;
    private final WechatAuthorizedAppService wechatAuthorizedAppService;
    private final DomainEventPublisher domainEventPublisher;
    private final UserDomainService userDomainService;
    private final MemberDomainService memberDomainService;
    private final AuthSessionManager authSessionManager;

    /**
     * 微信小程序登录/注册。
     * <p>
     * 流程：
     * 1. 根据 tenantId 查询授权的小程序 appId
     * 2. 使用 code 换取 openId、unionId、sessionKey
     * 3. 解密手机号（可选）
     * 4. 根据 unionId 或 (appId, openId) 注册或加载用户
     * 5. 确保租户会员存在
     * 6. 创建登录会话
     * </p>
     */
    public LoginResponse loginByWeChatMiniApp(WechatMiniAppLoginRequest cmd) {
        Long tenantId = cmd.getSourceTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("sourceTenantId 不能为空");
        }

        // 1. 根据 tenantId 查询授权的小程序 appId
        String appId = wechatAuthorizedAppService.getAuthorizerAppIdByTenantId(tenantId)
                .orElseThrow(() -> new IllegalStateException("租户未授权小程序，tenantId=" + tenantId));
        
        log.info("[UserAuth] WeChat mini app login, tenantId={}, appId={}", tenantId, appId);

        // 2. code 换 session
        WeChatCode2SessionResult sessionResult = weChatMiniAppClient.code2Session(appId, cmd.getCode());

        // 3. 解密手机号（可选）
        String phone = null;
        String countryCode = "+86";
        if (StringUtils.hasText(cmd.getPhoneCode())) {
            // 优先使用 phoneCode 方式（推荐）
            WeChatPhoneNumberResult phoneResult = weChatMiniAppClient.decryptPhoneNumber(
                    appId, cmd.getPhoneCode(), null, null);
            if (phoneResult != null) {
                phone = phoneResult.getPhoneNumber();
                countryCode = phoneResult.getCountryCode();
            }
        } else if (StringUtils.hasText(cmd.getEncryptedData()) && StringUtils.hasText(cmd.getIv())) {
            // 兼容旧版本 encryptedData/iv 方式
            WeChatPhoneNumberResult phoneResult = weChatMiniAppClient.decryptPhoneNumber(
                    appId, sessionResult.getSessionKey(), cmd.getEncryptedData(), cmd.getIv());
            if (phoneResult != null) {
                phone = phoneResult.getPhoneNumber();
                countryCode = phoneResult.getCountryCode();
            }
        }

        // 4. 注册或加载用户
        // 注意：如果 unionId 为空，可以考虑使用 openId 作为 unionId 的替代
        // 但这样会导致同一个用户在不同小程序中被识别为不同用户
        // 这里我们仍然使用 unionId（即使为空），让 UserDomainService 通过手机号识别用户
        String effectiveUnionId = sessionResult.getUnionId();
        if (!StringUtils.hasText(effectiveUnionId)) {
            // 如果 unionId 为空，使用 "openid:{appId}:{openId}" 作为临时标识
            // 这样可以保证同一个 openId 在同一个小程序中被识别为同一个用户
            effectiveUnionId = "openid:" + appId + ":" + sessionResult.getOpenId();
            log.warn("[UserAuth] unionId is empty, using openId-based identifier: {}", effectiveUnionId);
        } else {
            log.info("[UserAuth] Using unionId for user registration/load");
        }
        
        UserRegistrationResult registerResult = userDomainService.registerOrLoadByWeChatUnionId(
                effectiveUnionId,
                phone,
                countryCode,
                tenantId,
                RegisterChannel.WECHAT_MINI
        );
        UserIdentity identity = registerResult.identity();
        boolean isNewUser = registerResult.isNew();

        // 5. 发布用户注册事件
        if (isNewUser) {
            UserRegisteredEvent event = new UserRegisteredEvent(
                    identity.getId(),
                    identity.getFirstTenantId(),
                    identity.getUnionId(),
                    identity.getPhone(),
                    identity.getRegisterChannel() != null ? identity.getRegisterChannel().name() : null
            );
            domainEventPublisher.publish(event);
            log.info("[UserAuth] New user registered, userId={}, tenantId={}", identity.getId(), tenantId);
        }

        // 6. TODO: 更新画像（昵称、头像等）

        // 7. 确保会员存在
        TenantMember member = memberDomainService.ensureMemberForUser(tenantId, identity.getId(), "JOIN_WECHAT_AUTO");
        boolean isNewMember = member.getCreatedAt().isAfter(java.time.LocalDateTime.now().minusSeconds(5));

        // 8. 创建会话
        AuthSessionCreateResult session = authSessionManager.createSession(
                identity.getId(), tenantId, "MINIAPP", null, null, null);

        // 9. 组装结果
        LoginResponse result = new LoginResponse();
        result.setAccessToken(session.getAccessToken());
        result.setRefreshToken(session.getRefreshToken());
        result.setExpireAt(session.getExpireAt());
        result.setUserId(identity.getId());
        result.setTenantId(tenantId);
        result.setMemberId(member.getId());
        result.setNewUser(isNewUser);
        result.setNewMember(isNewMember);
        
        log.info("[UserAuth] Login success, userId={}, tenantId={}, memberId={}, newUser={}, newMember={}",
                identity.getId(), tenantId, member.getId(), isNewUser, isNewMember);
        
        return result;
    }
}
