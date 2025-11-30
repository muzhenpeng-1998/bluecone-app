package com.bluecone.app.gateway.routing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import com.bluecone.app.gateway.ApiContract;
import com.bluecone.app.gateway.endpoint.ApiEndpoint;
import com.bluecone.app.gateway.endpoint.ApiVersion;
import com.bluecone.app.gateway.handler.auth.AuthLoginHandler;
import com.bluecone.app.gateway.handler.auth.AuthRefreshHandler;
import com.bluecone.app.gateway.handler.order.OrderDetailHandler;

import jakarta.annotation.PostConstruct;

/**
 * Registry that maps HTTP routes to API contracts.
 */
@Component
public class ApiRouteRegistry {

    private final List<ApiRoute> routes = new ArrayList<>();

    public void register(ApiContract contract) {
        routes.add(new ApiRoute(contract.getHttpMethod(), contract.getPath(), contract));
    }

    public void registerAll(List<ApiContract> contracts) {
        contracts.forEach(this::register);
    }

    public Optional<ApiRoute> findRoute(HttpMethod method, String path) {
        return routes.stream()
                .filter(route -> route.matches(method, path))
                .findFirst();
    }

    public List<ApiRoute> getRoutes() {
        return Collections.unmodifiableList(routes);
    }

    @PostConstruct
    public void initDefaultRoutes() {
        // Authentication login
        register(ApiContract.builder()
                .code(ApiEndpoint.AUTH_LOGIN.getCode())
                .httpMethod(HttpMethod.POST)
                .path("/api/gw/auth/login")
                .version(ApiVersion.V1.getCode())
                .authRequired(false)
                .tenantRequired(false)
                .rateLimitEnabled(true)
                .rateLimitKey("auth.login")
                .handlerClass(AuthLoginHandler.class)
                .requestType(com.bluecone.app.api.auth.dto.LoginRequest.class)
                .description("User login via JWT")
                .build());

        // Refresh token
        register(ApiContract.builder()
                .code(ApiEndpoint.AUTH_REFRESH.getCode())
                .httpMethod(HttpMethod.POST)
                .path("/api/gw/auth/refresh")
                .version(ApiVersion.V1.getCode())
                .authRequired(false)
                .tenantRequired(false)
                .rateLimitEnabled(true)
                .rateLimitKey("auth.refresh")
                .handlerClass(AuthRefreshHandler.class)
                .requestType(com.bluecone.app.api.auth.dto.RefreshTokenRequest.class)
                .description("Refresh JWT access token")
                .build());

        // Order detail
        register(ApiContract.builder()
                .code(ApiEndpoint.ORDER_DETAIL.getCode())
                .httpMethod(HttpMethod.GET)
                .path("/api/gw/orders/{id}")
                .version(ApiVersion.V1.getCode())
                .authRequired(true)
                .tenantRequired(true)
                .rateLimitEnabled(true)
                .rateLimitKey("order.detail")
                .handlerClass(OrderDetailHandler.class)
                .description("Order detail v1")
                .build());
    }
}
