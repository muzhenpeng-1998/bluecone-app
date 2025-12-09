package com.bluecone.app.resource.api.enums;

/**
 * 资源档案编码，用于区分不同业务场景的存储策略。
 */
public enum ResourceProfileCode {

    /**
     * 门店主标识/Logo。
     */
    STORE_LOGO,

    /**
     * 商品展示图集。
     */
    PRODUCT_IMAGE,

    /**
     * 导出报表等临时文件。
     */
    EXPORT_REPORT,

    /**
     * 系统级临时资源。
     */
    SYSTEM_TEMP,

    /**
     * 用户头像图片。
     */
    USER_AVATAR
}
