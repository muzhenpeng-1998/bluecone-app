package com.bluecone.app.core.apicontract;

/**
 * Types of contextual data that can be resolved for a request.
 */
public enum ContextType {

    /**
     * Store context, e.g. tenant-bound store snapshot.
     */
    STORE,

    /**
     * User context, e.g. authenticated user snapshot.
     */
    USER,

    /**
     * Product context, e.g. resolved product identifiers or snapshot.
     */
    PRODUCT,

    /**
     * Inventory context, e.g. resolved inventory policy snapshot.
     */
    INVENTORY
}

