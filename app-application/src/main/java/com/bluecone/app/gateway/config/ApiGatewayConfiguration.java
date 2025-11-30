package com.bluecone.app.gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway auto-configuration wiring properties and component scan.
 */
@Configuration
@EnableConfigurationProperties(ApiGatewayProperties.class)
public class ApiGatewayConfiguration {
}
