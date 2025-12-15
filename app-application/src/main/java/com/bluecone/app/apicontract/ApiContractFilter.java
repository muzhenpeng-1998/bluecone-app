package com.bluecone.app.apicontract;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;

import com.bluecone.app.config.ApiContractProperties;
import com.bluecone.app.core.apicontract.ApiSide;
import com.bluecone.app.core.apicontract.ContractMatch;
import com.bluecone.app.core.apicontract.ContextType;
import com.bluecone.app.core.apicontract.RouteContract;
import com.bluecone.app.core.apicontract.RouteContractMatcher;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Front-line filter that determines the API side and required contexts
 * for the current request based on {@link RouteContract}s.
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class ApiContractFilter extends OncePerRequestFilter {

    public static final String ATTR_CONTRACT_MATCH = "bluecone.api.contract.match";
    public static final String ATTR_API_SIDE = "bluecone.api.contract.side";
    public static final String ATTR_REQUIRED_CONTEXTS = "bluecone.api.contract.requiredContexts";
    public static final String ATTR_OPTIONAL_CONTEXTS = "bluecone.api.contract.optionalContexts";

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final ApiContractProperties properties;
    private final RouteContractMatcher matcher;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.isEnabled()) {
            return true;
        }
        String path = request.getRequestURI();
        return isHardExcluded(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        ContractMatch match = matcher.match(path).orElse(null);
        if (match == null) {
            if (log.isDebugEnabled()) {
                log.debug("No API contract matched for path={}, skipping context routing", path);
            }
            filterChain.doFilter(request, response);
            return;
        }

        RouteContract contract = match.contract();
        ApiSide side = match.side();
        EnumSet<ContextType> required = EnumSet.copyOf(nonNull(contract.requiredContexts()));
        EnumSet<ContextType> optional = EnumSet.copyOf(nonNull(contract.optionalContexts()));

        // Expose match info via request attributes for downstream filters/middleware.
        request.setAttribute(ATTR_CONTRACT_MATCH, match);
        request.setAttribute(ATTR_API_SIDE, side);
        request.setAttribute(ATTR_REQUIRED_CONTEXTS, required);
        request.setAttribute(ATTR_OPTIONAL_CONTEXTS, optional);

        // Write minimal MDC for debugging and observability.
        String contractName = buildContractName(match);
        String originalSide = MDC.get("apiSide");
        String originalContract = MDC.get("apiContract");
        try {
            MDC.put("apiSide", side.name());
            MDC.put("apiContract", contractName);
            if (log.isDebugEnabled()) {
                log.debug("Matched API contract for path={} side={} required={} optional={}",
                        path, side, required, optional);
            }
            filterChain.doFilter(request, response);
        } finally {
            if (originalSide != null) {
                MDC.put("apiSide", originalSide);
            } else {
                MDC.remove("apiSide");
            }
            if (originalContract != null) {
                MDC.put("apiContract", originalContract);
            } else {
                MDC.remove("apiContract");
            }
        }
    }

    private boolean isHardExcluded(String path) {
        if (path == null) {
            return false;
        }
        return PATH_MATCHER.match("/ops/**", path)
                || PATH_MATCHER.match("/actuator/**", path);
    }

    private Set<ContextType> nonNull(Set<ContextType> set) {
        return set != null ? set : EnumSet.noneOf(ContextType.class);
    }

    private String buildContractName(ContractMatch match) {
        RouteContract contract = match.contract();
        String firstPattern = contract.includePatterns().isEmpty()
                ? "*"
                : contract.includePatterns().get(0);
        return match.side().name() + ":" + firstPattern;
    }
}

