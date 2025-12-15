package com.bluecone.app.core.cacheinval.guard;

/**
 * Aggregate decision made by {@link InvalidationStormGuard}.
 *
 * @param decision      decision type
 * @param stormMode     whether the tenant+namespace is currently in storm mode
 * @param reason        human-readable reason for observability
 * @param effectiveEpoch epoch value when {@link InvalidationDecision#EPOCH_BUMP}
 * @param keysCount     number of keys in the original event
 */
public record GuardDecision(
        InvalidationDecision decision,
        boolean stormMode,
        String reason,
        long effectiveEpoch,
        int keysCount
) {
}

