package com.bluecone.app.gateway.routing;

import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import com.bluecone.app.gateway.ApiContract;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Route binding between HTTP request and API contract.
 */
@Getter
@RequiredArgsConstructor
public class ApiRoute {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final HttpMethod httpMethod;
    private final String pathPattern;
    private final ApiContract contract;

    public boolean matches(HttpMethod method, String path) {
        return method == httpMethod && PATH_MATCHER.match(pathPattern, normalize(path));
    }

    private String normalize(String path) {
        if (!StringUtils.hasText(path)) {
            return "/";
        }
        return path;
    }
}
