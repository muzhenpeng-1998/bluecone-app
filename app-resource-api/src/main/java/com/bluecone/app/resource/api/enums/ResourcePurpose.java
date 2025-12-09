package com.bluecone.app.resource.api.enums;

/**
 * 资源用途枚举，供业务侧指定具体场景。
 */
public enum ResourcePurpose {

    /**
     * 主要 Logo 或代表性图片。
     */
    MAIN_LOGO,

    /**
     * 图集形式的资源，如商品详情图集。
     */
    GALLERY,

    /**
     * 横幅、Banner 图。
     */
    BANNER,

    /**
     * 用户头像。
     */
    AVATAR,

    /**
     * 商品详情展示图。
     */
    DETAIL_IMAGE,

    /**
     * 其他辅助资源或业务自定义。
     */
    AUXILIARY
}
