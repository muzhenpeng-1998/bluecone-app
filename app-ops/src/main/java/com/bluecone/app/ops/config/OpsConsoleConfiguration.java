package com.bluecone.app.ops.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Ops console configuration bootstrap.
 */
@Configuration
@EnableConfigurationProperties(BlueconeOpsProperties.class)
public class OpsConsoleConfiguration {
}

