package com.bluecone.app.core.config.domain;

import com.bluecone.app.core.config.Feature;

/**
 * Tenant plan configuration describing entitlements and limits.
 */
public interface TenantPlanConfig {

    String planLevel();

    boolean hasFeature(Feature feature);

    int maxStores();

    int maxUsers();
}
