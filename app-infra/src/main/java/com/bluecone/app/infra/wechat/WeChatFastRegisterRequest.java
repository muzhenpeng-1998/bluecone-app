package com.bluecone.app.infra.wechat;

import lombok.Data;

/**
 * fastregisterweapp 请求入参。
 *
 * 字段命名尽量贴近微信文档。
 */
@Data
public class WeChatFastRegisterRequest {

    /**
     * 企业/个体工商户名称：name。
     */
    private String name;

    /**
     * 企业代码：统一社会信用代码 / 组织机构代码 / 营业执照注册号。
     */
    private String code;

    /**
     * 企业代码类型：1=统一社会信用代码；2=组织机构代码；3=营业执照注册号。
     */
    private Integer codeType;

    /**
     * 法人微信号：legal_persona_wechat。
     */
    private String legalPersonaWechat;

    /**
     * 法人姓名：legal_persona_name。
     */
    private String legalPersonaName;

    /**
     * 第三方联系电话：component_phone，可选。
     */
    private String componentPhone;

    /**
     * 预留扩展字段，用于透传额外参数（如行业、地区等）。
     */
    private String extJson;
}

