package com.bluecone.app.core.config;

/**
 * High-level feature gate to toggle product capabilities.
 */
public interface FeatureGate {

    boolean isOn(Feature feature);

    default boolean isOff(Feature feature) {
        return !isOn(feature);
    }
}
