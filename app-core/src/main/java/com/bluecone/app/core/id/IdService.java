package com.bluecone.app.core.id;

/**
 * ID service abstraction hiding generator details from business layers.
 */
public interface IdService {

    /**
     * Returns a raw ULID string.
     */
    String nextId();

    /**
     * Returns a typed ID with semantic prefix (e.g., usr_, ord_).
     */
    String nextId(IdType type);

    /**
     * Typed value object wrapper for downstream safety.
     */
    default TypedId nextTypedId(IdType type) {
        return TypedId.of(type, nextId(type));
    }
}
