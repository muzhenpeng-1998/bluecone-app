package com.bluecone.app.gateway;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpMethod;

import com.bluecone.app.gateway.endpoint.ApiEndpoint;

/**
 * Lightweight mapping from HTTP method + path to logical endpoint codes.
 */
public class ApiRequestMapping {

    private final Map<RouteKey, ApiEndpoint> mapping = new ConcurrentHashMap<>();

    public void register(HttpMethod method, String path, ApiEndpoint endpoint) {
        mapping.put(new RouteKey(method, path), endpoint);
    }

    public Optional<ApiEndpoint> find(HttpMethod method, String path) {
        return Optional.ofNullable(mapping.get(new RouteKey(method, path)));
    }

    private record RouteKey(HttpMethod method, String path) {
    }
}
