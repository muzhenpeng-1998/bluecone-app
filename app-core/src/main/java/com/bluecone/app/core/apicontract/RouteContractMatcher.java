package com.bluecone.app.core.apicontract;

import java.util.Optional;

/**
 * Strategy for matching a concrete request path to a {@link RouteContract}.
 */
public interface RouteContractMatcher {

    /**
     * Attempt to match the given request path to a configured {@link RouteContract}.
     *
     * @param path raw request path (e.g. {@code /api/mini/order/confirm})
     * @return first matching contract, or {@link Optional#empty()} if none matched
     */
    Optional<ContractMatch> match(String path);
}

