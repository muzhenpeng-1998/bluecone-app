package com.bluecone.app.tenant.application.wechat;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bluecone.app.infra.wechat.openplatform.WechatAuthorizedAppDO;
import com.bluecone.app.infra.wechat.dataobject.WechatRegisterTaskDO;
import com.bluecone.app.infra.wechat.openplatform.WechatAuthorizedAppMapper;
import com.bluecone.app.infra.wechat.mapper.WechatRegisterTaskMapper;
import com.bluecone.app.store.dao.entity.BcStore;
import com.bluecone.app.store.dao.mapper.BcStoreMapper;
import com.bluecone.app.tenant.application.wechat.command.WechatAuthorizedEventCommand;
import com.bluecone.app.tenant.application.wechat.command.WechatUnauthorizedEventCommand;
import com.bluecone.app.tenant.dao.entity.Tenant;
import com.bluecone.app.tenant.dao.mapper.TenantMapper;
import com.bluecone.app.infra.wechat.openplatform.WechatComponentCredentialService;
import com.bluecone.app.wechat.config.WeChatOpenPlatformProperties;
import me.chanjar.weixin.common.util.crypto.WxCryptUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 微信开放平台回调应用服务。
 * <p>
 * 负责处理小程序授权/更新授权/取消授权事件，更新本地授权表以及租户/门店状态，不直接解析 HTTP 回调报文。
 * Controller 解析出事件类型与字段后调用本服务。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class WechatOpenCallbackAppService {

    private static final Logger log = LoggerFactory.getLogger(WechatOpenCallbackAppService.class);

    private final WechatAuthorizedAppMapper wechatAuthorizedAppMapper;
    private final WechatRegisterTaskMapper wechatRegisterTaskMapper;
    private final TenantMapper tenantMapper;
    private final BcStoreMapper bcStoreMapper;
    private final WechatComponentCredentialService wechatComponentCredentialService;
    private final WeChatOpenPlatformProperties weChatOpenPlatformProperties;

    /**
     * 微信开放平台回调原始入口。
     * <p>
     * Controller 将签名参数与原始请求体透传到此方法，后续在这里做消息解密与事件解析，
     * 再分发到 handleMiniProgramAuthorized / handleMiniProgramUnauthorized 等具体验证逻辑。
     * </p>
     *
     * @param signature    微信签名（URL 上的 signature）
     * @param timestamp    时间戳
     * @param nonce        随机串
     * @param msgSignature 加密消息签名（msg_signature）
     * @param requestBody  原始请求体（加密 XML）
     */
    public void handleRawCallback(String signature, String timestamp, String nonce, String msgSignature, String requestBody) {
        log.info("[WechatOpenCallback] raw callback received, signature={}, timestamp={}, nonce={}, msgSignature={}, bodyLength={}",
                signature, timestamp, nonce, msgSignature,
                requestBody != null ? requestBody.length() : 0);

        // 1. 解密消息
        String decryptedXml;
        try {
            String componentToken = weChatOpenPlatformProperties.getComponentToken();
            String componentAesKey = weChatOpenPlatformProperties.getComponentAesKey();
            String componentAppId = weChatOpenPlatformProperties.getComponentAppId();

            if (!StringUtils.hasText(componentToken) || !StringUtils.hasText(componentAesKey) || !StringUtils.hasText(componentAppId)) {
                log.error("[WechatOpenCallback] 配置缺失：componentToken/componentAesKey/componentAppId");
                return;
            }

            WxCryptUtil cryptUtil = new WxCryptUtil(componentToken, componentAesKey, componentAppId);
            decryptedXml = cryptUtil.decrypt(requestBody);
            
            log.debug("[WechatOpenCallback] 消息解密成功，明文长度={}", decryptedXml != null ? decryptedXml.length() : 0);
        } catch (Exception e) {
            log.error("[WechatOpenCallback] 消息解密失败: {}", e.getMessage(), e);
            return;
        }

        // 2. 解析 XML 获取 InfoType 和 AuthorizerAppid
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(decryptedXml)));
            Element root = doc.getDocumentElement();

            String infoType = getElementText(root, "InfoType");
            String authorizerAppid = getElementText(root, "AuthorizerAppid");

            log.info("[WechatOpenCallback] 解析事件：InfoType={}, AuthorizerAppid={}", infoType, authorizerAppid);

            // 3. 根据 InfoType 路由到不同的处理方法
            if (!StringUtils.hasText(infoType)) {
                log.warn("[WechatOpenCallback] InfoType 为空，无法路由");
                return;
            }

            switch (infoType) {
                case "component_verify_ticket":
                    // 处理 component_verify_ticket 事件
                    handleComponentVerifyTicket(root);
                    break;

                case "unauthorized":
                    // 处理取消授权事件
                    if (StringUtils.hasText(authorizerAppid)) {
                        WechatUnauthorizedEventCommand command = new WechatUnauthorizedEventCommand(authorizerAppid);
                        handleMiniProgramUnauthorized(command);
                    } else {
                        log.warn("[WechatOpenCallback] unauthorized 事件缺少 AuthorizerAppid");
                    }
                    break;

                case "authorized":
                case "updateauthorized":
                    // 处理授权/更新授权事件
                    // 注意：raw event 没有 state，无法直接绑定 tenantId
                    // 这里只记录日志，实际绑定在授权回调页面完成
                    log.info("[WechatOpenCallback] 收到 {} 事件，AuthorizerAppid={}（需要在授权回调页面完成绑定）", 
                            infoType, authorizerAppid);
                    // TODO: 可以触发异步任务刷新 authorizer_access_token
                    break;

                default:
                    log.info("[WechatOpenCallback] 未处理的 InfoType: {}", infoType);
                    break;
            }

        } catch (Exception e) {
            log.error("[WechatOpenCallback] XML 解析失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理 component_verify_ticket 事件。
     */
    private void handleComponentVerifyTicket(Element root) {
        String ticket = getElementText(root, "ComponentVerifyTicket");
        if (StringUtils.hasText(ticket)) {
            wechatComponentCredentialService.saveOrUpdateVerifyTicket(ticket);
            log.info("[WechatOpenCallback] component_verify_ticket 已保存");
        } else {
            log.warn("[WechatOpenCallback] component_verify_ticket 为空");
        }
    }

    /**
     * 从 XML Element 中提取文本内容。
     */
    private String getElementText(Element parent, String tagName) {
        try {
            org.w3c.dom.NodeList nodeList = parent.getElementsByTagName(tagName);
            if (nodeList.getLength() > 0) {
                org.w3c.dom.Node node = nodeList.item(0);
                return node.getTextContent();
            }
        } catch (Exception e) {
            log.debug("[WechatOpenCallback] 提取 {} 失败: {}", tagName, e.getMessage());
        }
        return null;
    }

    /**
     * 处理微信开放平台 authorized / updateauthorized 事件。
     * <p>
     * 根据 authorizerAppid 维护 bc_wechat_authorized_app 记录，并更新租户/门店的入驻状态与默认小程序 appid。
     * 同时尝试将相关注册任务标记为成功。
     * </p>
     *
     * @param command 授权/更新授权事件命令
     */
    @Transactional
    public void handleMiniProgramAuthorized(WechatAuthorizedEventCommand command) {
        String appid = command.authorizerAppid();
        if (!StringUtils.hasText(appid)) {
            log.warn("[WechatOpenCallback] authorized event missing authorizerAppid");
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        // 1）查找已有授权记录
        WechatAuthorizedAppDO existing = wechatAuthorizedAppMapper.selectOne(
                new QueryWrapper<WechatAuthorizedAppDO>()
                        .eq("authorizer_app_id", appid)
                        .last("LIMIT 1"));

        WechatAuthorizedAppDO appRecord;

        if (existing == null) {
            // 2）首次授权：尝试通过注册任务反查租户/门店
            WechatRegisterTaskDO task = wechatRegisterTaskMapper.selectOne(
                    new QueryWrapper<WechatRegisterTaskDO>()
                            .eq("authorizer_appid", appid)
                            .orderByDesc("created_at")
                            .last("LIMIT 1"));

            appRecord = new WechatAuthorizedAppDO();
            if (task != null) {
                appRecord.setTenantId(task.getTenantId());
                // Note: storeId is not in the new DO, skip
            }
            appRecord.setCreatedAt(now);

            applyAuthorizedFields(appRecord, command, now, true);

            wechatAuthorizedAppMapper.insert(appRecord);
            log.info("[WechatOpenCallback] created authorized app record, appid={}, tenantId={}",
                    appid, appRecord.getTenantId());
        } else {
            // 3）更新授权信息
            appRecord = existing;
            applyAuthorizedFields(appRecord, command, now, false);

            wechatAuthorizedAppMapper.updateById(appRecord);
            log.info("[WechatOpenCallback] updated authorized app record, appid={}, tenantId={}",
                    appid, appRecord.getTenantId());
        }

        // 4）更新租户默认小程序与入驻状态
        updateTenantOnAuthorization(appRecord, appid);

        // 5）更新门店默认小程序与入驻状态
        updateStoreOnAuthorization(appRecord, appid);

        // 6）将相关注册任务标记为成功（如果存在处理中任务）
        markRegisterTasksAsSuccess(appid);
    }

    /**
     * 处理微信开放平台 unauthorized 事件。
     * <p>
     * 将授权记录标记为已取消，并清理租户/门店上绑定的对应小程序 appid（不修改入驻状态）。
     * </p>
     *
     * @param command 取消授权事件命令
     */
    @Transactional
    public void handleMiniProgramUnauthorized(WechatUnauthorizedEventCommand command) {
        String appid = command.authorizerAppid();
        if (!StringUtils.hasText(appid)) {
            log.warn("[WechatOpenCallback] unauthorized event missing authorizerAppid");
            return;
        }

        WechatAuthorizedAppDO appRecord = wechatAuthorizedAppMapper.selectOne(
                new QueryWrapper<WechatAuthorizedAppDO>()
                        .eq("authorizer_app_id", appid)
                        .last("LIMIT 1"));
        if (appRecord == null) {
            log.warn("[WechatOpenCallback] unauthorized event for unknown appid={}", appid);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        appRecord.setAuthorizationStatus("UNAUTHORIZED"); // 已取消
        appRecord.setUnauthorizedAt(now);
        appRecord.setUpdatedAt(now);
        wechatAuthorizedAppMapper.updateById(appRecord);

        // 清理租户默认小程序 appid（如匹配当前被取消的 appid）
        if (appRecord.getTenantId() != null) {
            Tenant tenant = tenantMapper.selectById(appRecord.getTenantId());
            if (tenant != null && Objects.equals(tenant.getDefaultMiniappAppid(), appid)) {
                tenant.setDefaultMiniappAppid(null);
                tenantMapper.updateById(tenant);
                log.info("[WechatOpenCallback] cleared tenant.defaultMiniappAppid for tenantId={} appid={}",
                        tenant.getId(), appid);
            }
        }

        // Note: storeId is not in the new DO, skip store-level cleanup

        // TODO: 如需对注册任务做额外处理，可在此处补充（例如标记为取消）
    }

    private void applyAuthorizedFields(WechatAuthorizedAppDO appRecord,
                                       WechatAuthorizedEventCommand command,
                                       LocalDateTime now,
                                       boolean isNew) {
        appRecord.setAuthorizerAppId(command.authorizerAppid());
        if (StringUtils.hasText(command.authorizerRefreshToken())) {
            appRecord.setAuthorizerRefreshToken(command.authorizerRefreshToken());
        }
        appRecord.setNickName(command.nickName());
        appRecord.setHeadImg(command.headImg());
        // principalType -> not in new DO, skip
        appRecord.setPrincipalName(command.principalName());
        // signature -> not in new DO, skip
        appRecord.setServiceTypeInfo(command.serviceType());
        appRecord.setVerifyTypeInfo(command.verifyType());
        // funcInfoJson -> not in new DO, skip
        appRecord.setBusinessInfo(command.businessInfoJson());
        appRecord.setMiniProgramInfo(command.miniprograminfoJson());

        // 授权状态：AUTHORIZED
        appRecord.setAuthorizationStatus("AUTHORIZED");
        // certStatus -> not in new DO, skip

        if (isNew || appRecord.getAuthorizedAt() == null) {
            appRecord.setAuthorizedAt(now);
        }
        // lastAuthUpdateAt -> not in new DO, use updatedAt
        appRecord.setUpdatedAt(now);
    }

    private Integer resolveCertStatus(Integer verifyType, Integer existing) {
        if (verifyType == null || verifyType == 0) {
            return existing != null ? existing : 0;
        }
        return 1;
    }

    private void updateTenantOnAuthorization(WechatAuthorizedAppDO appRecord, String appid) {
        Long tenantId = appRecord.getTenantId();
        if (tenantId == null) {
            return;
        }
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            log.warn("[WechatOpenCallback] tenant not found for tenantId={} appid={}", tenantId, appid);
            return;
        }

        boolean changed = false;
        // 入驻状态：0-草稿 -> 1-入驻完成
        if (tenant.getOnboardStatus() != null && tenant.getOnboardStatus() == 0) {
            tenant.setOnboardStatus(1);
            changed = true;
        }
        // 默认小程序 appid 为空时，设置为当前授权 appid
        if (!StringUtils.hasText(tenant.getDefaultMiniappAppid())) {
            tenant.setDefaultMiniappAppid(appid);
            changed = true;
        }

        if (changed) {
            tenantMapper.updateById(tenant);
            log.info("[WechatOpenCallback] updated tenant on authorization, tenantId={}, onboardStatus={}, defaultMiniappAppid={}",
                    tenant.getId(), tenant.getOnboardStatus(), tenant.getDefaultMiniappAppid());
        }
    }

    private void updateStoreOnAuthorization(WechatAuthorizedAppDO appRecord, String appid) {
        // Note: storeId is not in the new DO, this method is now a no-op
        // If store-level authorization is needed, it should be tracked differently
        log.debug("[WechatOpenCallback] updateStoreOnAuthorization skipped (storeId not in new schema)");
    }

    private void markRegisterTasksAsSuccess(String appid) {
        List<WechatRegisterTaskDO> processingTasks = wechatRegisterTaskMapper.selectList(
                new QueryWrapper<WechatRegisterTaskDO>()
                        .eq("authorizer_appid", appid)
                        .eq("status", 1));
        if (processingTasks == null || processingTasks.isEmpty()) {
            return;
        }

        for (WechatRegisterTaskDO task : processingTasks) {
            task.setStatus(2); // 成功
            task.setFailCode(null);
            task.setFailReason(null);
            wechatRegisterTaskMapper.updateById(task);
        }

        log.info("[WechatOpenCallback] marked {} register tasks as success for appid={}",
                processingTasks.size(), appid);
    }
}
