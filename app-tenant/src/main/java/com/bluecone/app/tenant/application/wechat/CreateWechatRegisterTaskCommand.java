package com.bluecone.app.tenant.application.wechat;

/**
 * 创建微信小程序注册任务命令。
 * 仅用于入驻引导流程中，根据会话创建注册任务记录。
 */
public record CreateWechatRegisterTaskCommand(
        // 入驻会话 token
        String sessionToken,
        // 注册类型：FORMAL / TRIAL
        String registerType,
        // 发给微信的注册参数 JSON 快照（透传给开放平台网关）
        String requestPayloadJson,
        // 企业/个体户名称，对应 fastregisterweapp.name
        String companyName,
        // 企业代码：统一社会信用代码/营业执照号等，对应 fastregisterweapp.code
        String companyCode,
        // 企业代码类型：1=统一社会信用代码，2=组织机构代码，3=营业执照注册号
        Integer companyCodeType,
        // 法人微信号：legal_persona_wechat
        String legalPersonaWechat,
        // 法人姓名：legal_persona_name
        String legalPersonaName,
        // 试用小程序昵称，fastregisterbetaweapp.name，默认可用品牌名
        String trialMiniProgramName,
        // 试用小程序联系人 openid，fastregisterbetaweapp.openid
        String trialOpenId) {
}
