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
     * 流程（新版本，不再信任客户端传 tenantId）：
     * 1. 以 authorizerAppId（小程序 appId）为主键，从 bc_wechat_authorized_app 反查 tenantId
     * 2. 使用 code 换取 openId、unionId、sessionKey
     * 3. 解密手机号（可选）
     * 4. 根据 unionId / phone / (appId, openId) 注册或加载用户
     * 5. 确保租户会员存在
     * 6. 创建登录会话
     * </p>
     */
    public LoginResponse loginByWeChatMiniApp(WechatMiniAppLoginRequest cmd) {
        String appId = cmd.getAuthorizerAppId();
        if (!StringUtils.hasText(appId)) {
            throw new IllegalArgumentException("authorizerAppId 不能为空");
        }

        // 1. 以 authorizerAppId 为主键，从 bc_wechat_authorized_app 反查 tenantId
        var authorizedApp = wechatAuthorizedAppService.getAuthorizedAppByAppId(appId)
                .orElseThrow(() -> new IllegalStateException("小程序未接入或未授权，appId=" + appId));
        
        if (!"AUTHORIZED".equals(authorizedApp.getAuthorizationStatus())) {
            throw new IllegalStateException("小程序授权状态异常，appId=" + appId + ", status=" + authorizedApp.getAuthorizationStatus());
        }
        
        Long tenantId = authorizedApp.getTenantId();
        log.info("[UserAuth] WeChat mini app login, appId={}, tenantId={}", appId, tenantId);

        // 2. code 换 session
        WeChatCode2SessionResult sessionResult = weChatMiniAppClient.code2Session(appId, cmd.getCode());
        String openId = sessionResult.getOpenId();
        String unionId = sessionResult.getUnionId();
        
        log.debug("[UserAuth] code2Session success, openId前3后3={}, unionId={}", 
                maskOpenId(openId), 
                StringUtils.hasText(unionId) ? "有值" : "空");

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
                log.debug("[UserAuth] 获取手机号成功（phoneCode）");
            }
        } else if (StringUtils.hasText(cmd.getEncryptedData()) && StringUtils.hasText(cmd.getIv())) {
            // 兼容旧版本 encryptedData/iv 方式
            WeChatPhoneNumberResult phoneResult = weChatMiniAppClient.decryptPhoneNumber(
                    appId, sessionResult.getSessionKey(), cmd.getEncryptedData(), cmd.getIv());
            if (phoneResult != null) {
                phone = phoneResult.getPhoneNumber();
                countryCode = phoneResult.getCountryCode();
                log.debug("[UserAuth] 获取手机号成功（encryptedData/iv）");
            }
        }

        // 4. 注册或加载用户（使用新的 registerOrLoadByWeChatMiniApp 方法）
        // 识别优先级：unionId > phone > external_identity(appId, openId)
        UserRegistrationResult registerResult = userDomainService.registerOrLoadByWeChatMiniApp(
                unionId,
                appId,
                openId,
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

    /**
     * 脱敏 openId：只显示前3后3字符。
     */
    private String maskOpenId(String openId) {
        if (openId == null || openId.length() <= 6) {
            return "***";
        }
        return openId.substring(0, 3) + "***" + openId.substring(openId.length() - 3);
    }
}

