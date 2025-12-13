package com.bluecone.app.core.contextkit;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * 缓存值抽象，支持命中与负缓存。
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = HitValue.class, name = "hit"),
        @JsonSubTypes.Type(value = NegativeValue.class, name = "negative")
})
public sealed interface CacheValue permits HitValue, NegativeValue {
}

