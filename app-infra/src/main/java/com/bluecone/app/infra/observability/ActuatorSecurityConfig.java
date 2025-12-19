package com.bluecone.app.infra.observability;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

import java.util.Arrays;
import java.util.List;

/**
 * Actuator Security Configuration
 * 
 * Restricts access to Actuator endpoints to internal networks only.
 * 
 * Security Rules:
 * 1. Health and Info endpoints: accessible from anywhere (for load balancer health checks)
 * 2. Prometheus and other sensitive endpoints: restricted to internal network only
 * 3. Internal network: 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16, 127.0.0.0/8
 * 
 * Configuration:
 * - bluecone.observability.allowed-networks: comma-separated list of CIDR blocks
 * - Default: 10.0.0.0/8,172.16.0.0/12,192.168.0.0/16,127.0.0.0/8
 */
@Slf4j
@Configuration
@EnableWebSecurity
@Order(1) // Higher priority than default security config
public class ActuatorSecurityConfig {

    @Value("${bluecone.observability.allowed-networks:10.0.0.0/8,172.16.0.0/12,192.168.0.0/16,127.0.0.0/8}")
    private String allowedNetworks;

    @Bean
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/internal/actuator/**")
            .authorizeHttpRequests(authorize -> authorize
                // Health and Info endpoints: accessible from anywhere
                .requestMatchers(EndpointRequest.to(HealthEndpoint.class, InfoEndpoint.class))
                .permitAll()
                
                // All other actuator endpoints: restricted to internal network
                .requestMatchers(EndpointRequest.toAnyEndpoint())
                .access((authentication, context) -> {
                    HttpServletRequest request = context.getRequest();
                    String remoteAddr = getClientIpAddress(request);
                    
                    boolean allowed = isInternalNetwork(remoteAddr);
                    
                    if (!allowed) {
                        log.warn("[ActuatorSecurity] Access denied to {} from IP: {}", 
                                request.getRequestURI(), remoteAddr);
                    }
                    
                    return new org.springframework.security.authorization.AuthorizationDecision(allowed);
                })
            )
            .csrf(csrf -> csrf.disable()); // Disable CSRF for actuator endpoints (read-only)

        return http.build();
    }

    /**
     * Check if the IP address is in the internal network
     */
    private boolean isInternalNetwork(String ipAddress) {
        List<String> networks = Arrays.asList(allowedNetworks.split(","));
        
        for (String network : networks) {
            IpAddressMatcher matcher = new IpAddressMatcher(network.trim());
            if (matcher.matches(ipAddress)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Get client IP address from request
     * Handles X-Forwarded-For header for proxied requests
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
