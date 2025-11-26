package com.bluecone.app.core.api;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

/**
 * API 请求封装对象
 * 统一封装 HTTP 请求信息，供 Handler 使用
 */
public class ApiRequest {

    private final HttpServletRequest rawRequest;
    private final String method;
    private final String path;
    private final Map<String, String[]> queryParams;
    private final Map<String, String> headers;
    private final String body;
    private final int version;
    private final Long tenantId;
    private final Long userId;

    private ApiRequest(Builder builder) {
        this.rawRequest = builder.rawRequest;
        this.method = builder.method;
        this.path = builder.path;
        this.queryParams = builder.queryParams;
        this.headers = builder.headers;
        this.body = builder.body;
        this.version = builder.version;
        this.tenantId = builder.tenantId;
        this.userId = builder.userId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public HttpServletRequest getRawRequest() {
        return rawRequest;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String[]> getQueryParams() {
        return queryParams;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    public int getVersion() {
        return version;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getQueryParam(String name) {
        String[] values = queryParams.get(name);
        return values != null && values.length > 0 ? values[0] : null;
    }

    public String getHeader(String name) {
        return headers.get(name);
    }

    public static class Builder {
        private HttpServletRequest rawRequest;
        private String method;
        private String path;
        private Map<String, String[]> queryParams;
        private Map<String, String> headers;
        private String body;
        private int version;
        private Long tenantId;
        private Long userId;

        public Builder rawRequest(HttpServletRequest rawRequest) {
            this.rawRequest = rawRequest;
            return this;
        }

        public Builder method(String method) {
            this.method = method;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder queryParams(Map<String, String[]> queryParams) {
            this.queryParams = queryParams;
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public Builder version(int version) {
            this.version = version;
            return this;
        }

        public Builder tenantId(Long tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public ApiRequest build() {
            return new ApiRequest(this);
        }
    }
}
