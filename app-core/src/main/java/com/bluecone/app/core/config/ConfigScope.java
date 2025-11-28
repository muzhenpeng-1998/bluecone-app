package com.bluecone.app.core.config;

/**
 * Supported configuration scopes in the layered ConfigCenter.
 * SYSTEM is the global default, ENV is environment specific, TENANT is per tenant.
 */
public enum ConfigScope {
    SYSTEM,
    ENV,
    TENANT
}
