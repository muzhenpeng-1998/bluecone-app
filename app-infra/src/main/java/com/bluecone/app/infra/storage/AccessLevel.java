package com.bluecone.app.infra.storage;

/**
 * 访问级别。
 */
public enum AccessLevel {

    /**
     * 私有，仅签名可访问。
     */
    PRIVATE,

    /**
     * 公共读。
     */
    PUBLIC_READ,

    /**
     * 内部可见（预留）。
     */
    INTERNAL
}

