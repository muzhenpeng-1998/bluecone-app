package com.bluecone.app.core.id;

import java.util.Objects;

/**
 * Value object carrying both semantic type and concrete ID string.
 */
public record TypedId(IdType type, String value) {

    public TypedId {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(value, "value must not be null");
    }

    public static TypedId of(IdType type, String value) {
        return new TypedId(type, value);
    }

    @Override
    public String toString() {
        return value;
    }
}
