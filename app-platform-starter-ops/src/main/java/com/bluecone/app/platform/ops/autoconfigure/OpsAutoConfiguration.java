package com.bluecone.app.platform.ops.autoconfigure;

import com.bluecone.app.ops.config.BlueconeOpsProperties;
import com.bluecone.app.ops.config.OpsConsoleConfiguration;
import com.bluecone.app.ops.config.OpsWebMvcConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Ops Console Auto-Configuration.
 * 
 * <p>Disabled by default. Enable with {@code bluecone.ops.enabled=true}.</p>
 * 
 * <p>Ops routes (/ops/**, /actuator/**) are excluded from business context middleware
 * to avoid pollution.</p>
 */
@AutoConfiguration
@ConditionalOnClass({WebMvcConfigurer.class})
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "bluecone.ops", name = "enabled", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(BlueconeOpsProperties.class)
@Import({OpsConsoleConfiguration.class, OpsWebMvcConfiguration.class})
public class OpsAutoConfiguration {
    // Configuration imported from app-ops module
}

