package com.bluecone.app.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * External configuration for the embedded API gateway.
 */
@Data
@ConfigurationProperties(prefix = "bluecone.gateway")
public class ApiGatewayProperties {

    /**
     * Toggle gateway routing. When disabled, controller can short-circuit.
     */
    private boolean enabled = true;

    /**
     * Shared secret for simple HMAC signature verification.
     */
    private String signatureSecret = "bluecone-signature-dev";

    /**
     * Allowed clock skew for signature timestamp.
     */
    private long signatureToleranceSeconds = 300;
}
