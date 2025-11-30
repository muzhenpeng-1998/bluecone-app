package com.bluecone.app.gateway;

import java.time.Duration;

import org.springframework.http.HttpMethod;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Declarative API contract that describes non-business concerns for a single endpoint.
 */
@Getter
@Builder(toBuilder = true)
@ToString
public class ApiContract {

    private final String code;
    private final HttpMethod httpMethod;
    private final String path;
    private final String version;
    @Builder.Default
    private final boolean authRequired = false;
    @Builder.Default
    private final boolean tenantRequired = false;
    @Builder.Default
    private final boolean rateLimitEnabled = false;
    private final String rateLimitKey;
    @Builder.Default
    private final int rateLimitCapacity = 30;
    @Builder.Default
    private final Duration rateLimitWindow = Duration.ofSeconds(1);
    @Builder.Default
    private final boolean idempotent = false;
    private final String idempotentKeyExpression;
    @Builder.Default
    private final Duration idempotentTtl = Duration.ofMinutes(10);
    @Builder.Default
    private final boolean signatureRequired = false;
    private final Class<? extends ApiHandler<?, ?>> handlerClass;
    private final Class<?> requestType;
    private final String description;
}
