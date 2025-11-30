package com.bluecone.app.gateway.endpoint;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Semantic endpoint identifiers decoupled from URLs.
 */
@Getter
@RequiredArgsConstructor
public enum ApiEndpoint {

    AUTH_LOGIN("auth.login"),
    AUTH_REFRESH("auth.refresh"),
    ORDER_DETAIL("order.detail"),
    ORDER_CREATE("order.create"),
    HEALTH_PING("health.ping");

    private final String code;
}
