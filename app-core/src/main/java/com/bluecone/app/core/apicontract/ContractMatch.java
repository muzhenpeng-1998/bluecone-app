package com.bluecone.app.core.apicontract;

/**
 * Result of matching an incoming request path against configured contracts.
 *
 * @param side     resolved API side
 * @param contract matched contract definition
 * @param path     concrete request path
 */
public record ContractMatch(
        ApiSide side,
        RouteContract contract,
        String path
) {
}

