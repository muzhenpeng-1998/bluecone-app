package com.bluecone.app.member.application.auth;

import com.bluecone.app.core.event.DomainEventPublisher;
import com.bluecone.app.core.user.domain.event.UserRegisteredEvent;
import com.bluecone.app.core.user.domain.identity.RegisterChannel;
import com.bluecone.app.core.user.domain.identity.UserIdentity;
import com.bluecone.app.core.user.domain.member.TenantMember;
import com.bluecone.app.core.user.domain.service.MemberDomainService;
import com.bluecone.app.core.user.domain.service.UserDomainService;
import com.bluecone.app.core.user.domain.service.UserDomainService.UserRegistrationResult;
import com.bluecone.app.wechat.facade.miniapp.*;
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

    private final WeChatMiniAppFacade weChatMiniAppFacade;
    private final WechatAuthorizedAppService wechatAuthorizedAppService;
    private final DomainEventPublisher domainEventPublisher;
    private final UserDomainService userDomainService;
    private final MemberDomainService memberDomainService;
    private final AuthSessionManager authSessionManager;

    /**
     * 微信小程序登录/注册。
     * <p>
     * 流程（Phase 3 版本，使用 facade + tenantId 路由）：
     * 1. 使用 tenantId 通过 facade 换取 openId、unionId、sessionKey
     * 2. 获取手机号（可选，通过 facade）
     * 3. 根据 unionId / phone / (appId, openId) 注册或加载用户
     * 4. 确保租户会员存在
     * 5. 创建登录会话
     * </p>
     */
    public LoginResponse loginByWeChatMiniApp(WechatMiniAppLoginRequest cmd) {
        Long tenantId = cmd.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId 不能为空");
        }

        log.info("[UserAuth] WeChat mini app login, tenantId={}", tenantId);

        // 1. code 换 session（通过 facade，内部会路由到 authorizerAppId）
        WeChatMiniAppCode2SessionCommand code2SessionCmd = WeChatMiniAppCode2SessionCommand.builder()
                .tenantId(tenantId)
                .storeId(cmd.getStoreId())
                .code(cmd.getCode())
                .build();
        
        WeChatMiniAppLoginResult sessionResult = weChatMiniAppFacade.code2Session(code2SessionCmd);
        String openId = sessionResult.getOpenId();
        String unionId = sessionResult.getUnionId();
        
        log.debug("[UserAuth] code2Session success, openId前3后3={}, unionId={}", 
                maskOpenId(openId), 
                StringUtils.hasText(unionId) ? "有值" : "空");

        // 2. 获取 authorizerAppId（用于用户身份关联）
        String appId = wechatAuthorizedAppService.getAuthorizerAppIdByTenantId(tenantId)
                .orElseThrow(() -> new IllegalStateException("租户未授权小程序，tenantId=" + tenantId));

        // 3. 获取手机号（可选，通过 facade）
        String phone = null;
        String countryCode = "+86";
        if (StringUtils.hasText(cmd.getPhoneCode())) {
            try {
                WeChatMiniAppPhoneCommand phoneCmd = WeChatMiniAppPhoneCommand.builder()
                        .tenantId(tenantId)
                        .storeId(cmd.getStoreId())
                        .phoneCode(cmd.getPhoneCode())
                        .build();
                
                WeChatMiniAppPhoneResult phoneResult = weChatMiniAppFacade.getPhoneNumber(phoneCmd);
                if (phoneResult != null) {
                    phone = phoneResult.getPhoneNumber();
                    countryCode = phoneResult.getCountryCode();
                    log.info("[UserAuth] 获取手机号成功（phoneCode方式）");
                } else {
                    log.warn("[UserAuth] 获取手机号失败（phoneCode方式返回null）");
                }
            } catch (Exception e) {
                log.error("[UserAuth] 获取手机号失败（phoneCode方式）: {}", e.getMessage());
            }
        } else {
            log.debug("[UserAuth] 未提供手机号参数（phoneCode），跳过手机号获取");
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

