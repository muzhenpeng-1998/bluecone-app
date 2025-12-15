package com.bluecone.app.apicontract;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.bluecone.app.config.ApiContractProperties;
import com.bluecone.app.core.apicontract.ApiSide;
import com.bluecone.app.core.apicontract.ContractMatch;
import com.bluecone.app.core.apicontract.ContextType;
import com.bluecone.app.core.apicontract.RouteContract;
import com.bluecone.app.core.apicontract.RouteContractMatcher;
import org.springframework.util.AntPathMatcher;

/**
 * Default implementation backed by {@link AntPathMatcher}.
 * <p>
 * Contracts are evaluated in declaration order and the first match wins.
 */
public class DefaultRouteContractMatcher implements RouteContractMatcher {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final List<RouteContract> contracts;

    public DefaultRouteContractMatcher(ApiContractProperties properties) {
        this.contracts = toContracts(properties);
    }

    @Override
    public Optional<ContractMatch> match(String path) {
        if (path == null) {
            return Optional.empty();
        }
        for (RouteContract contract : contracts) {
            if (matches(contract, path)) {
                return Optional.of(new ContractMatch(contract.side(), contract, path));
            }
        }
        return Optional.empty();
    }

    private boolean matches(RouteContract contract, String path) {
        boolean included = false;
        for (String pattern : contract.includePatterns()) {
            if (PATH_MATCHER.match(pattern, path)) {
                included = true;
                break;
            }
        }
        if (!included) {
            return false;
        }
        for (String pattern : contract.excludePatterns()) {
            if (PATH_MATCHER.match(pattern, path)) {
                return false;
            }
        }
        return true;
    }

    private List<RouteContract> toContracts(ApiContractProperties properties) {
        List<RouteContract> list = new ArrayList<>();
        if (properties == null || properties.getRoutes() == null) {
            return List.of();
        }
        for (ApiContractProperties.RouteContractProperties rp : properties.getRoutes()) {
            if (rp == null || rp.getSide() == null) {
                continue;
            }
            ApiSide side = rp.getSide();
            List<String> includes = rp.getIncludePatterns() != null ? rp.getIncludePatterns() : List.of();
            List<String> excludes = rp.getExcludePatterns() != null ? rp.getExcludePatterns() : List.of();
            EnumSet<ContextType> required = rp.getRequiredContexts() != null
                    ? EnumSet.copyOf(rp.getRequiredContexts())
                    : EnumSet.noneOf(ContextType.class);
            EnumSet<ContextType> optional = rp.getOptionalContexts() != null
                    ? EnumSet.copyOf(rp.getOptionalContexts())
                    : EnumSet.noneOf(ContextType.class);
            // Ensure required/optional sets are never null.
            Objects.requireNonNull(required, "requiredContexts");
            Objects.requireNonNull(optional, "optionalContexts");
            list.add(new RouteContract(side, includes, excludes, required, optional));
        }
        return List.copyOf(list);
    }
}

