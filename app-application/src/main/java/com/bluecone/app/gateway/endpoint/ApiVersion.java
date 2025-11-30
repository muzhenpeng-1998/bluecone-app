package com.bluecone.app.gateway.endpoint;

/**
 * API version markers.
 */
public enum ApiVersion {
    V1("v1"),
    V2("v2");

    private final String code;

    ApiVersion(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
