package com.bluecone.app.core.apicontract;

/**
 * Logical side of an API.
 *
 * <ul>
 *     <li>{@link #USER} - end user / mini program facing APIs</li>
 *     <li>{@link #MERCHANT} - merchant / operator facing APIs</li>
 *     <li>{@link #PLATFORM} - platform admin / ops / actuator style APIs</li>
 * </ul>
 */
public enum ApiSide {

    /**
     * 小程序 / C 端用户侧。
     */
    USER,

    /**
     * 商户 / B 端控制台。
     */
    MERCHANT,

    /**
     * 平台管理 / 运维后台（包含 admin/ops/actuator 等）。
     */
    PLATFORM
}

