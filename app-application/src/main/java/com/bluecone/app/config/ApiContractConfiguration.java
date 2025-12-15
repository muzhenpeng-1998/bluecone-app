package com.bluecone.app.config;

import com.bluecone.app.apicontract.DefaultRouteContractMatcher;
import com.bluecone.app.core.apicontract.RouteContractMatcher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration wiring API route contract matcher.
 */
@Configuration
public class ApiContractConfiguration {

    @Bean
    public RouteContractMatcher routeContractMatcher(ApiContractProperties properties) {
        return new DefaultRouteContractMatcher(properties);
    }
}

