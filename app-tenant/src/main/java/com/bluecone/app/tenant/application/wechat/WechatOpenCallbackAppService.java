package com.bluecone.app.tenant.application.wechat;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bluecone.app.infra.wechat.dataobject.WechatAuthorizedAppDO;
import com.bluecone.app.infra.wechat.dataobject.WechatRegisterTaskDO;
import com.bluecone.app.infra.wechat.mapper.WechatAuthorizedAppMapper;
import com.bluecone.app.infra.wechat.mapper.WechatRegisterTaskMapper;
import com.bluecone.app.store.dao.entity.BcStore;
import com.bluecone.app.store.dao.mapper.BcStoreMapper;
import com.bluecone.app.tenant.application.wechat.command.WechatAuthorizedEventCommand;
import com.bluecone.app.tenant.application.wechat.command.WechatUnauthorizedEventCommand;
import com.bluecone.app.tenant.dao.entity.Tenant;
import com.bluecone.app.tenant.dao.mapper.TenantMapper;
import com.bluecone.app.infra.wechat.openplatform.WechatComponentCredentialService;
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
        // TODO: 在这里接入微信开放平台消息解密与事件解析逻辑，并分发到 handleMiniProgramAuthorized/handleMiniProgramUnauthorized 等方法
        // 例如，当解析出 event 为 component_verify_ticket 时：
        // String ticket = parsedXml.getComponentVerifyTicket();
        // wechatComponentCredentialService.saveOrUpdateVerifyTicket(ticket);
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
                        .eq("authorizer_appid", appid)
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
                appRecord.setStoreId(task.getStoreId());
            }

            applyAuthorizedFields(appRecord, command, now, true);

            wechatAuthorizedAppMapper.insert(appRecord);
            log.info("[WechatOpenCallback] created authorized app record, appid={}, tenantId={}, storeId={}",
                    appid, appRecord.getTenantId(), appRecord.getStoreId());
        } else {
            // 3）更新授权信息
            appRecord = existing;
            applyAuthorizedFields(appRecord, command, now, false);

            wechatAuthorizedAppMapper.updateById(appRecord);
            log.info("[WechatOpenCallback] updated authorized app record, appid={}, tenantId={}, storeId={}",
                    appid, appRecord.getTenantId(), appRecord.getStoreId());
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
                        .eq("authorizer_appid", appid)
                        .last("LIMIT 1"));
        if (appRecord == null) {
            log.warn("[WechatOpenCallback] unauthorized event for unknown appid={}", appid);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        appRecord.setAuthStatus(2); // 已取消
        appRecord.setCanceledAt(now);
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

        // 清理门店级小程序 appid（如匹配当前被取消的 appid）
        if (appRecord.getStoreId() != null) {
            BcStore store = bcStoreMapper.selectById(appRecord.getStoreId());
            if (store != null && Objects.equals(store.getMiniappAppid(), appid)) {
                store.setMiniappAppid(null);
                bcStoreMapper.updateById(store);
                log.info("[WechatOpenCallback] cleared store.miniappAppid for storeId={} appid={}",
                        store.getId(), appid);
            }
        }

        // TODO: 如需对注册任务做额外处理，可在此处补充（例如标记为取消）
    }

    private void applyAuthorizedFields(WechatAuthorizedAppDO appRecord,
                                       WechatAuthorizedEventCommand command,
                                       LocalDateTime now,
                                       boolean isNew) {
        appRecord.setAuthorizerAppid(command.authorizerAppid());
        if (StringUtils.hasText(command.authorizerRefreshToken())) {
            appRecord.setAuthorizerRefreshToken(command.authorizerRefreshToken());
        }
        appRecord.setNickName(command.nickName());
        appRecord.setHeadImg(command.headImg());
        appRecord.setPrincipalType(command.principalType());
        appRecord.setPrincipalName(command.principalName());
        appRecord.setSignature(command.signature());
        appRecord.setServiceType(command.serviceType());
        appRecord.setVerifyType(command.verifyType());
        appRecord.setFuncInfoJson(command.funcInfoJson());
        appRecord.setBusinessInfoJson(command.businessInfoJson());
        appRecord.setMiniprograminfoJson(command.miniprograminfoJson());

        // 授权状态：1-已授权
        appRecord.setAuthStatus(1);
        // 认证状态粗略推导：verifyType != 0 视为已认证
        appRecord.setCertStatus(resolveCertStatus(command.verifyType(), appRecord.getCertStatus()));

        if (isNew || appRecord.getFirstAuthTime() == null) {
            appRecord.setFirstAuthTime(now);
        }
        appRecord.setLastAuthUpdateAt(now);
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
        Long storeId = appRecord.getStoreId();
        if (storeId == null) {
            return;
        }
        BcStore store = bcStoreMapper.selectById(storeId);
        if (store == null) {
            log.warn("[WechatOpenCallback] store not found for storeId={} appid={}", storeId, appid);
            return;
        }

        boolean changed = false;
        // 入驻状态：0-草稿 -> 1-可营业（READY）
        if (store.getOnboardStatus() != null && store.getOnboardStatus() == 0) {
            store.setOnboardStatus(1);
            changed = true;
        }
        // 门店级小程序 appid 为空时，设置为当前授权 appid
        if (!StringUtils.hasText(store.getMiniappAppid())) {
            store.setMiniappAppid(appid);
            changed = true;
        }

        if (changed) {
            bcStoreMapper.updateById(store);
            log.info("[WechatOpenCallback] updated store on authorization, storeId={}, onboardStatus={}, miniappAppid={}",
                    store.getId(), store.getOnboardStatus(), store.getMiniappAppid());
        }
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
