package com.bluecone.app.tenant.application.wechat;

import com.bluecone.app.core.error.BizErrorCode;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.infra.tenant.dataobject.TenantOnboardingSessionDO;
import com.bluecone.app.infra.wechat.WeChatBetaRegisterRequest;
import com.bluecone.app.infra.wechat.WeChatBetaRegisterResult;
import com.bluecone.app.infra.wechat.WeChatFastRegisterClient;
import com.bluecone.app.infra.wechat.WeChatFastRegisterRequest;
import com.bluecone.app.infra.wechat.WeChatFastRegisterResult;
import com.bluecone.app.infra.wechat.dataobject.WechatRegisterTaskDO;
import com.bluecone.app.infra.wechat.mapper.WechatRegisterTaskMapper;
import com.bluecone.app.tenant.service.TenantOnboardingAppService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 微信小程序注册任务应用服务。
 * <p>
 * 根据入驻会话和注册参数，调用微信开放平台注册接口，并在 bc_wechat_register_task 中落一条任务记录。
 * 成功时任务状态为处理中（1），失败时为失败（3）。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class WechatMiniProgramRegisterAppService {

    private static final Logger log = LoggerFactory.getLogger(WechatMiniProgramRegisterAppService.class);

    private final TenantOnboardingAppService tenantOnboardingAppService;
    private final WechatRegisterTaskMapper wechatRegisterTaskMapper;
    private final WeChatFastRegisterClient weChatFastRegisterClient;

    /**
     * 根据入驻会话和注册类型创建一条微信小程序注册任务记录，并调用微信开放平台注册接口。
     * <p>
     * 正常情况下会落一条状态为“处理中”的任务记录，后续由回调/轮询更新最终状态；
     * 如调用异常，则记录失败状态（3）及错误原因。
     * </p>
     *
     * @param command 创建注册任务命令
     * @return 新建任务的主键 ID
     */
    @Transactional
    public Long createRegisterTask(CreateWechatRegisterTaskCommand command) {
        TenantOnboardingSessionDO session = tenantOnboardingAppService.findBySessionToken(command.sessionToken());
        if (session == null) {
            throw new BusinessException(BizErrorCode.RESOURCE_NOT_FOUND, "入驻会话不存在或已失效");
        }
        Long tenantId = session.getTenantId();
        if (tenantId == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "入驻会话尚未绑定租户，无法发起注册任务");
        }

        String registerType = command.registerType();
        if (!StringUtils.hasText(registerType)) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "registerType 不能为空");
        }
        String type = registerType.toUpperCase();
        if (!("FORMAL".equals(type) || "TRIAL".equals(type))) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "注册类型仅支持 FORMAL 或 TRIAL");
        }

        WechatRegisterTaskDO task = new WechatRegisterTaskDO();
        task.setTenantId(tenantId);
        task.setStoreId(session.getStoreId());
        task.setRegisterType(type);
        String requestPayloadJson = StringUtils.hasText(command.requestPayloadJson())
                ? command.requestPayloadJson()
                : "{}";
        task.setRequestPayloadJson(requestPayloadJson);
        task.setNotifyCount(0);

        try {
            if ("TRIAL".equals(type)) {
                WeChatBetaRegisterRequest req = new WeChatBetaRegisterRequest();
                String betaName = StringUtils.hasText(command.trialMiniProgramName())
                        ? command.trialMiniProgramName()
                        : "trial-miniapp";
                // TODO: 可使用租户品牌名/门店名作为默认昵称
                req.setName(betaName);
                req.setOpenId(command.trialOpenId());
                req.setExtJson(command.requestPayloadJson());

                log.info("[MiniAppRegister] calling fastregisterbetaweapp, tenantId={}, storeId={}, name={}, openId={}",
                        tenantId, session.getStoreId(), req.getName(), req.getOpenId());

                WeChatBetaRegisterResult result = weChatFastRegisterClient.fastRegisterBetaWeapp(req);
                if (result != null) {
                    if (StringUtils.hasText(result.getAppId())) {
                        task.setAuthorizerAppid(result.getAppId());
                    }
                    task.setResponsePayloadJson(result.getRawBody());
                }
                task.setStatus(1); // 处理中
            } else {
                WeChatFastRegisterRequest req = new WeChatFastRegisterRequest();
                req.setName(command.companyName());
                req.setCode(command.companyCode());
                req.setCodeType(command.companyCodeType());
                req.setLegalPersonaWechat(command.legalPersonaWechat());
                req.setLegalPersonaName(command.legalPersonaName());
                req.setComponentPhone(session.getContactPhone());
                req.setExtJson(command.requestPayloadJson());

                log.info("[MiniAppRegister] calling fastregisterweapp, tenantId={}, storeId={}, name={}, code={}, codeType={}",
                        tenantId, session.getStoreId(), req.getName(), req.getCode(), req.getCodeType());

                WeChatFastRegisterResult result = weChatFastRegisterClient.fastRegisterWeapp(req);
                if (result != null) {
                    if (StringUtils.hasText(result.getAppId())) {
                        task.setAuthorizerAppid(result.getAppId());
                    }
                    task.setResponsePayloadJson(result.getRawBody());
                }
                task.setStatus(1); // 处理中
            }
        } catch (Exception ex) {
            log.warn("[MiniAppRegister] call WeChat open platform failed, tenantId={}, storeId={}, type={}, error={}",
                    tenantId, session.getStoreId(), type, ex.getMessage(), ex);
            task.setStatus(3); // 失败
            task.setFailCode("HTTP_ERROR");
            task.setFailReason(ex.getMessage());
            // TODO: 根据需要决定是否向上抛出业务异常，当前仅记录失败状态
        }

        wechatRegisterTaskMapper.insert(task);
        return task.getId();
    }

    /**
     * 标记任务为处理中（预留扩展点，后续补充实现）。
     */
    public void markProcessing(Long taskId, String wechatTaskNo, String responsePayloadJson) {
        // TODO: 实现任务状态更新为处理中，并回填 wechatTaskNo / responsePayloadJson
    }

    /**
     * 标记任务为成功（预留扩展点，后续补充实现）。
     */
    public void markSuccess(Long taskId, String authorizerAppid) {
        // TODO: 实现任务状态更新为成功，并回填 authorizerAppid
    }

    /**
     * 标记任务为失败（预留扩展点，后续补充实现）。
     */
    public void markFailed(Long taskId, String failCode, String failReason) {
        // TODO: 实现任务状态更新为失败，并记录失败原因
    }
}
