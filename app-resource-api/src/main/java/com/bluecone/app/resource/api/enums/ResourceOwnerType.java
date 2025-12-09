package com.bluecone.app.resource.api.enums;

/**
 * 资源归属类型定义，用于标识资源绑定的业务维度。
 */
public enum ResourceOwnerType {

    /**
     * 门店。
     */
    STORE,

    /**
     * 商品。
     */
    PRODUCT,

    /**
     * 用户。
     */
    USER,

    /**
     * 租户。
     */
    TENANT,

    /**
     * 系统级别资源。
     */
    SYSTEM
}
