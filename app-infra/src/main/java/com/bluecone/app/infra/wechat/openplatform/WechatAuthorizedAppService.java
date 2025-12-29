package com.bluecone.app.infra.wechat.openplatform;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 微信已授权小程序服务。
 * <p>
 * 提供查询租户授权小程序信息的能力，以及 authorizer_access_token 的刷新管理。
 * </p>
 * <p>
 * 注意：使用 @Lazy 注入 WeChatOpenPlatformClient 以打破循环依赖。
 * </p>
 */
@Service
public class WechatAuthorizedAppService {

    private static final Logger log = LoggerFactory.getLogger(WechatAuthorizedAppService.class);

    private final WechatAuthorizedAppMapper authorizedAppMapper;
    private final WeChatOpenPlatformClient weChatOpenPlatformClient;
    private final WechatComponentCredentialService wechatComponentCredentialService;

    /**
     * 构造函数，使用 @Lazy 注入 WeChatOpenPlatformClient 以打破循环依赖。
     */
    public WechatAuthorizedAppService(
            WechatAuthorizedAppMapper authorizedAppMapper,
            @Lazy WeChatOpenPlatformClient weChatOpenPlatformClient,
            WechatComponentCredentialService wechatComponentCredentialService) {
        this.authorizedAppMapper = authorizedAppMapper;
        this.weChatOpenPlatformClient = weChatOpenPlatformClient;
        this.wechatComponentCredentialService = wechatComponentCredentialService;
    }

    /**
     * 根据租户 ID 查询已授权的小程序 AppID。
     * <p>
     * 如果一个租户授权了多个小程序，返回第一个（通常一个租户只授权一个小程序）。
     * </p>
     *
     * @param tenantId 租户 ID
     * @return 小程序 AppID，如果未找到返回 Optional.empty()
     */
    public Optional<String> getAuthorizerAppIdByTenantId(Long tenantId) {
        if (tenantId == null) {
            log.warn("[WechatAuthorizedAppService] tenantId is null");
            return Optional.empty();
        }

        WechatAuthorizedAppDO authorizedApp = authorizedAppMapper.selectOne(
                new QueryWrapper<WechatAuthorizedAppDO>()
                        .eq("tenant_id", tenantId)
                        .eq("authorization_status", "AUTHORIZED")
                        .orderByDesc("authorized_at")
                        .last("LIMIT 1")
        );

        if (authorizedApp == null) {
            log.warn("[WechatAuthorizedAppService] No authorized app found for tenantId={}", tenantId);
            return Optional.empty();
        }

        String appId = authorizedApp.getAuthorizerAppId();
        log.info("[WechatAuthorizedAppService] Found authorized app for tenantId={}, appId={}***", 
                tenantId, appId != null && appId.length() > 6 ? appId.substring(0, 6) : "");
        
        return Optional.of(appId);
    }

    /**
     * 根据小程序 AppID 查询授权信息。
     *
     * @param authorizerAppId 小程序 AppID
     * @return 授权信息，如果未找到返回 Optional.empty()
     */
    public Optional<WechatAuthorizedAppDO> getAuthorizedAppByAppId(String authorizerAppId) {
        if (authorizerAppId == null) {
            log.warn("[WechatAuthorizedAppService] authorizerAppId is null");
            return Optional.empty();
        }

        WechatAuthorizedAppDO authorizedApp = authorizedAppMapper.selectOne(
                new QueryWrapper<WechatAuthorizedAppDO>()
                        .eq("authorizer_app_id", authorizerAppId)
                        .eq("authorization_status", "AUTHORIZED")
                        .last("LIMIT 1")
        );

        if (authorizedApp == null) {
            log.warn("[WechatAuthorizedAppService] No authorized app found for appId={}", authorizerAppId);
            return Optional.empty();
        }

        return Optional.of(authorizedApp);
    }

    /**
     * 根据租户 ID 和小程序 AppID 查询授权信息。
     *
     * @param tenantId        租户 ID
     * @param authorizerAppId 小程序 AppID
     * @return 授权信息，如果未找到返回 Optional.empty()
     */
    public Optional<WechatAuthorizedAppDO> getAuthorizedApp(Long tenantId, String authorizerAppId) {
        if (tenantId == null || authorizerAppId == null) {
            log.warn("[WechatAuthorizedAppService] tenantId or authorizerAppId is null");
            return Optional.empty();
        }

        WechatAuthorizedAppDO authorizedApp = authorizedAppMapper.selectOne(
                new QueryWrapper<WechatAuthorizedAppDO>()
                        .eq("tenant_id", tenantId)
                        .eq("authorizer_app_id", authorizerAppId)
                        .eq("authorization_status", "AUTHORIZED")
                        .last("LIMIT 1")
        );

        if (authorizedApp == null) {
            log.warn("[WechatAuthorizedAppService] No authorized app found for tenantId={}, appId={}", 
                    tenantId, authorizerAppId);
            return Optional.empty();
        }

        return Optional.of(authorizedApp);
    }

    /**
     * 根据小程序 AppID 查询授权信息（不限状态）。
     *
     * @param authorizerAppId 小程序 AppID
     * @return 授权信息，如果未找到返回 Optional.empty()
     */
    public Optional<WechatAuthorizedAppDO> getByAuthorizerAppId(String authorizerAppId) {
        if (authorizerAppId == null) {
            log.warn("[WechatAuthorizedAppService] authorizerAppId is null");
            return Optional.empty();
        }

        WechatAuthorizedAppDO authorizedApp = authorizedAppMapper.selectOne(
                new QueryWrapper<WechatAuthorizedAppDO>()
                        .eq("authorizer_app_id", authorizerAppId)
                        .orderByDesc("authorized_at")
                        .last("LIMIT 1")
        );

        if (authorizedApp == null) {
            log.warn("[WechatAuthorizedAppService] No app found for appId={}", authorizerAppId);
            return Optional.empty();
        }

        return Optional.of(authorizedApp);
    }

    /**
     * 获取或刷新授权方的 access_token。
     * 
     * 如果 token 为空或即将过期（120秒内），则自动刷新。
     *
     * @param authorizerAppId 小程序 AppID
     * @return 有效的 authorizer_access_token
     */
    public String getOrRefreshAuthorizerAccessToken(String authorizerAppId) {
        WechatAuthorizedAppDO authorizedApp = getByAuthorizerAppId(authorizerAppId)
                .orElseThrow(() -> new IllegalStateException("授权信息不存在, appId=" + authorizerAppId));

        // 检查 token 是否有效（未过期且距离过期时间大于 120 秒）
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireAt = authorizedApp.getAuthorizerAccessTokenExpireAt();
        
        boolean needRefresh = expireAt == null 
                || expireAt.isBefore(now.plusSeconds(120))
                || authorizedApp.getAuthorizerAccessToken() == null;

        if (!needRefresh) {
            log.debug("[WechatAuthorizedAppService] authorizer_access_token 仍然有效, appId={}", authorizerAppId);
            return authorizedApp.getAuthorizerAccessToken();
        }

        // 刷新 token
        log.info("[WechatAuthorizedAppService] 刷新 authorizer_access_token, appId={}", authorizerAppId);
        
        String componentAppId = authorizedApp.getComponentAppId();
        String authorizerRefreshToken = authorizedApp.getAuthorizerRefreshToken();
        
        if (authorizerRefreshToken == null) {
            throw new IllegalStateException("authorizer_refresh_token 为空, 无法刷新, appId=" + authorizerAppId);
        }

        // 获取 component_access_token
        String componentAccessToken = wechatComponentCredentialService.getValidComponentAccessToken();

        // 调用刷新接口
        RefreshAuthorizerTokenResult refreshResult = weChatOpenPlatformClient.refreshAuthorizerToken(
                componentAccessToken,
                componentAppId,
                authorizerAppId,
                authorizerRefreshToken
        );

        // 更新数据库
        LocalDateTime newExpireAt = now.plusSeconds(refreshResult.getExpiresInSeconds());
        
        UpdateWrapper<WechatAuthorizedAppDO> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("authorizer_app_id", authorizerAppId)
                .set("authorizer_access_token", refreshResult.getAuthorizerAccessToken())
                .set("authorizer_refresh_token", refreshResult.getAuthorizerRefreshToken())
                .set("authorizer_access_token_expire_at", newExpireAt)
                .set("updated_at", now);
        
        int updated = authorizedAppMapper.update(null, updateWrapper);
        if (updated == 0) {
            log.warn("[WechatAuthorizedAppService] 更新 authorizer_access_token 失败, appId={}", authorizerAppId);
        } else {
            log.info("[WechatAuthorizedAppService] 更新 authorizer_access_token 成功, appId={}, expireAt={}", 
                    authorizerAppId, newExpireAt);
        }

        return refreshResult.getAuthorizerAccessToken();
    }

    /**
     * 保存或更新授权信息（用于回调事件处理）。
     * <p>
     * 如果记录不存在则创建，如果存在则更新基本信息和授权状态。
     * </p>
     *
     * @param authorizerAppId 小程序 AppID
     * @param authorizerInfo  授权方信息（从 getAuthorizerInfo 获取）
     */
    public void saveOrUpdateAuthorizerInfo(String authorizerAppId, AuthorizerInfoResult authorizerInfo) {
        if (authorizerAppId == null) {
            log.warn("[WechatAuthorizedAppService] authorizerAppId is null");
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        // 查找已有记录
        WechatAuthorizedAppDO existing = authorizedAppMapper.selectOne(
                new QueryWrapper<WechatAuthorizedAppDO>()
                        .eq("authorizer_app_id", authorizerAppId)
                        .last("LIMIT 1")
        );

        if (existing == null) {
            // 创建新记录
            WechatAuthorizedAppDO newRecord = new WechatAuthorizedAppDO();
            newRecord.setAuthorizerAppId(authorizerAppId);
            newRecord.setAuthorizationStatus("AUTHORIZED");
            newRecord.setAuthorizedAt(now);
            newRecord.setCreatedAt(now);
            newRecord.setUpdatedAt(now);

            if (authorizerInfo != null) {
                applyAuthorizerInfo(newRecord, authorizerInfo);
            }

            authorizedAppMapper.insert(newRecord);
            log.info("[WechatAuthorizedAppService] 创建授权记录, appId={}", authorizerAppId);
        } else {
            // 更新已有记录
            existing.setAuthorizationStatus("AUTHORIZED");
            if (existing.getAuthorizedAt() == null) {
                existing.setAuthorizedAt(now);
            }
            existing.setUpdatedAt(now);

            if (authorizerInfo != null) {
                applyAuthorizerInfo(existing, authorizerInfo);
            }

            authorizedAppMapper.updateById(existing);
            log.info("[WechatAuthorizedAppService] 更新授权记录, appId={}", authorizerAppId);
        }
    }

    /**
     * 保存或更新授权信息（包含 refresh_token，用于 queryAuth 后的处理）。
     *
     * @param authorizerAppId          小程序 AppID
     * @param authorizerRefreshToken   授权方刷新令牌
     * @param authorizerAccessToken    授权方接口调用令牌
     * @param expiresInSeconds         access_token 过期时间（秒）
     * @param authorizerInfo           授权方信息
     */
    public void saveOrUpdateAuthorizerWithTokens(
            String authorizerAppId,
            String authorizerRefreshToken,
            String authorizerAccessToken,
            Integer expiresInSeconds,
            AuthorizerInfoResult authorizerInfo) {
        if (authorizerAppId == null) {
            log.warn("[WechatAuthorizedAppService] authorizerAppId is null");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireAt = expiresInSeconds != null ? now.plusSeconds(expiresInSeconds) : null;

        // 查找已有记录
        WechatAuthorizedAppDO existing = authorizedAppMapper.selectOne(
                new QueryWrapper<WechatAuthorizedAppDO>()
                        .eq("authorizer_app_id", authorizerAppId)
                        .last("LIMIT 1")
        );

        if (existing == null) {
            // 创建新记录
            WechatAuthorizedAppDO newRecord = new WechatAuthorizedAppDO();
            newRecord.setAuthorizerAppId(authorizerAppId);
            newRecord.setAuthorizerRefreshToken(authorizerRefreshToken);
            newRecord.setAuthorizerAccessToken(authorizerAccessToken);
            newRecord.setAuthorizerAccessTokenExpireAt(expireAt);
            newRecord.setAuthorizationStatus("AUTHORIZED");
            newRecord.setAuthorizedAt(now);
            newRecord.setCreatedAt(now);
            newRecord.setUpdatedAt(now);

            if (authorizerInfo != null) {
                applyAuthorizerInfo(newRecord, authorizerInfo);
            }

            authorizedAppMapper.insert(newRecord);
            log.info("[WechatAuthorizedAppService] 创建授权记录（含 tokens）, appId={}", authorizerAppId);
        } else {
            // 更新已有记录
            if (authorizerRefreshToken != null) {
                existing.setAuthorizerRefreshToken(authorizerRefreshToken);
            }
            if (authorizerAccessToken != null) {
                existing.setAuthorizerAccessToken(authorizerAccessToken);
                existing.setAuthorizerAccessTokenExpireAt(expireAt);
            }
            existing.setAuthorizationStatus("AUTHORIZED");
            if (existing.getAuthorizedAt() == null) {
                existing.setAuthorizedAt(now);
            }
            existing.setUpdatedAt(now);

            if (authorizerInfo != null) {
                applyAuthorizerInfo(existing, authorizerInfo);
            }

            authorizedAppMapper.updateById(existing);
            log.info("[WechatAuthorizedAppService] 更新授权记录（含 tokens）, appId={}", authorizerAppId);
        }
    }

    /**
     * 标记授权为已取消。
     *
     * @param authorizerAppId 小程序 AppID
     */
    public void markAsUnauthorized(String authorizerAppId) {
        if (authorizerAppId == null) {
            log.warn("[WechatAuthorizedAppService] authorizerAppId is null");
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        UpdateWrapper<WechatAuthorizedAppDO> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("authorizer_app_id", authorizerAppId)
                .set("authorization_status", "UNAUTHORIZED")
                .set("unauthorized_at", now)
                .set("updated_at", now);

        int updated = authorizedAppMapper.update(null, updateWrapper);
        if (updated > 0) {
            log.info("[WechatAuthorizedAppService] 标记为已取消授权, appId={}, count={}", authorizerAppId, updated);
        } else {
            log.warn("[WechatAuthorizedAppService] 未找到授权记录, appId={}", authorizerAppId);
        }
    }

    /**
     * 应用授权方信息到 DO 对象。
     * <p>
     * 注意：AuthorizerInfoResult 只包含基本字段，更详细的信息需要通过其他接口获取。
     * </p>
     */
    private void applyAuthorizerInfo(WechatAuthorizedAppDO record, AuthorizerInfoResult info) {
        if (info.getNickName() != null) {
            record.setNickName(info.getNickName());
        }
        if (info.getHeadImg() != null) {
            record.setHeadImg(info.getHeadImg());
        }
        if (info.getPrincipalName() != null) {
            record.setPrincipalName(info.getPrincipalName());
        }
        if (info.getVerifyType() != null) {
            record.setVerifyTypeInfo(info.getVerifyType());
        }
        // 其他字段（userName, alias, qrcodeUrl, serviceTypeInfo, businessInfo, miniProgramInfo）
        // 需要通过其他接口获取，这里不设置
    }
}

