package com.bluecone.app.core.config;

/**
 * Context for resolving configuration, carrying environment and tenant information.
 */
public record ConfigContext(String env, Long tenantId) {
}
