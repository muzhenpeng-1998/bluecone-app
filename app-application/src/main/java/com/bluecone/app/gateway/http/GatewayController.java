package com.bluecone.app.gateway.http;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bluecone.app.gateway.ApiGateway;
import com.bluecone.app.gateway.config.ApiGatewayProperties;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * Single entrypoint for gateway-managed APIs.
 */
@RestController
@RequestMapping("/api/gw")
@RequiredArgsConstructor
public class GatewayController {

    private final ApiGateway apiGateway;
    private final ApiGatewayProperties properties;

    @RequestMapping("/**")
    public ResponseEntity<?> dispatch(HttpServletRequest request) {
        if (!properties.isEnabled()) {
            return ResponseEntity.notFound().build();
        }
        Object response = apiGateway.handle(request);
        return ResponseEntity.ok(response);
    }
}
