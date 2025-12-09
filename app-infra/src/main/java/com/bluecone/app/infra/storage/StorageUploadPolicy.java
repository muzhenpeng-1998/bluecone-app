package com.bluecone.app.infra.storage;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

/**
 * 直传策略返回信息。
 */
public class StorageUploadPolicy {

    private String uploadUrl;
    private String httpMethod = "PUT";
    private Instant expiresAt;
    private Map<String, String> headers = Collections.emptyMap();
    private Map<String, String> formFields = Collections.emptyMap();

    public String getUploadUrl() {
        return uploadUrl;
    }

    public void setUploadUrl(final String uploadUrl) {
        this.uploadUrl = uploadUrl;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(final String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(final Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(final Map<String, String> headers) {
        this.headers = headers;
    }

    public Map<String, String> getFormFields() {
        return formFields;
    }

    public void setFormFields(final Map<String, String> formFields) {
        this.formFields = formFields;
    }
}

