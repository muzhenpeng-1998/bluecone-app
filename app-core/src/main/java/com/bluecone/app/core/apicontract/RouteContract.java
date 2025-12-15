package com.bluecone.app.core.apicontract;

import java.util.EnumSet;
import java.util.List;

/**
 * Declarative route-level contract describing which contexts are needed
 * for a group of API paths.
 */
public record RouteContract(
        ApiSide side,
        List<String> includePatterns,
        List<String> excludePatterns,
        EnumSet<ContextType> requiredContexts,
        EnumSet<ContextType> optionalContexts
) {
}

