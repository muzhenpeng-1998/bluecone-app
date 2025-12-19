package com.bluecone.app.id.api;

/**
 * 业务资源类型枚举，用于生成对外公开 ID 的前缀。
 */
public enum ResourceType {

    TENANT("tnt"),
    STORE("sto"),
    ORDER("ord"),
    USER("usr"),
    PRODUCT("prd"),
    SKU("sku"),
    PAYMENT("pay"),
    WALLET_ACCOUNT("wac"),
    WALLET_LEDGER("wl"),
    WALLET_FREEZE("wfz"),
    WALLET_RECHARGE("wrc");

    private final String prefix;

    ResourceType(String prefix) {
        this.prefix = prefix;
    }

    /**
     * 返回该资源类型对应的前缀字符串，例如 tnt/sto/ord/usr。
     *
     * @return 前缀字符串
     */
    public String prefix() {
        return prefix;
    }
}
