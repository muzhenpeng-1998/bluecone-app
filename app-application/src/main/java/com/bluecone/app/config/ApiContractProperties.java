package com.bluecone.app.config;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.bluecone.app.core.apicontract.ApiSide;
import com.bluecone.app.core.apicontract.ContextType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * External configuration for API route contracts and context requirements.
 */
@Component
@ConfigurationProperties(prefix = "bluecone.api.contract")
public class ApiContractProperties {

    /**
     * Global toggle for contract-based context routing.
     */
    private boolean enabled = true;

    /**
     * Route-level contracts. Evaluated in declaration order.
     */
    private List<RouteContractProperties> routes = List.of();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<RouteContractProperties> getRoutes() {
        return routes;
    }

    public void setRoutes(List<RouteContractProperties> routes) {
        this.routes = routes;
    }

    /**
     * Configuration view for a single route contract.
     */
    public static class RouteContractProperties {

        /**
         * API side for this contract.
         */
        private ApiSide side;

        /**
         * Ant-style patterns that should participate in this contract.
         */
        private List<String> includePatterns = List.of();

        /**
         * Ant-style patterns that should be excluded even if included above.
         */
        private List<String> excludePatterns = List.of();

        /**
         * Context types that must be resolved successfully.
         */
        private Set<ContextType> requiredContexts = EnumSet.noneOf(ContextType.class);

        /**
         * Context types that are optional for this contract.
         */
        private Set<ContextType> optionalContexts = EnumSet.noneOf(ContextType.class);

        public ApiSide getSide() {
            return side;
        }

        public void setSide(ApiSide side) {
            this.side = side;
        }

        public List<String> getIncludePatterns() {
            return includePatterns;
        }

        public void setIncludePatterns(List<String> includePatterns) {
            this.includePatterns = includePatterns;
        }

        public List<String> getExcludePatterns() {
            return excludePatterns;
        }

        public void setExcludePatterns(List<String> excludePatterns) {
            this.excludePatterns = excludePatterns;
        }

        public Set<ContextType> getRequiredContexts() {
            return requiredContexts;
        }

        public void setRequiredContexts(Set<ContextType> requiredContexts) {
            this.requiredContexts = requiredContexts != null
                    ? EnumSet.copyOf(requiredContexts)
                    : EnumSet.noneOf(ContextType.class);
        }

        public Set<ContextType> getOptionalContexts() {
            return optionalContexts;
        }

        public void setOptionalContexts(Set<ContextType> optionalContexts) {
            this.optionalContexts = optionalContexts != null
                    ? EnumSet.copyOf(optionalContexts)
                    : EnumSet.noneOf(ContextType.class);
        }
    }
}

