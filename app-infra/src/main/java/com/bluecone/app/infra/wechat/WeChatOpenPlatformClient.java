package com.bluecone.app.infra.wechat;

/**
 * 微信开放平台网关接口：
 * - 试用小程序快速注册 fastregisterbetaweapp
 * - 主体小程序快速注册 fastregisterweapp
 * - 查询注册结果 fastregisterweapp.search 等接口
 *
 * 这里只定义能力，具体 HTTP 调用后续再接入真实实现。
 */
public interface WeChatOpenPlatformClient {

    /**
     * 调用 fastregisterbetaweapp，为个人微信号创建“试用小程序”。
     *
     * 典型入参：name, openid。
     */
    WeChatBetaRegisterResult fastRegisterBetaWeapp(WeChatBetaRegisterRequest request);

    /**
     * 调用 fastregisterweapp，为企业主体快速注册小程序。
     *
     * 文档字段参考：
     * - name                      企业/个体工商户名称
     * - code                      企业代码（统一社会信用代码/组织机构代码/营业执照号）
     * - code_type                 企业代码类型（1=统一社会信用代码，2=组织机构代码，3=营业执照注册号）
     * - legal_persona_wechat      法人微信号
     * - legal_persona_name        法人姓名
     * - component_phone           第三方联系电话
     */
    WeChatFastRegisterResult fastRegisterWeapp(WeChatFastRegisterRequest request);

    /**
     * 查询快速注册小程序结果，用于轮询注册/审核状态。
     *
     * 常见实现是调用 fastregisterweapp.search，使用 name + legalPersonaWechat + legalPersonaName 查询。
     */
    WeChatRegisterStatusResult queryRegisterStatus(WeChatRegisterStatusQuery query);
}

