package com.bluecone.app.core.id;

public enum IdType {

    USER("usr"),
    ORDER("ord"),
    PAYMENT("pay"),
    TENANT("ten");

    private final String prefix;

    IdType(String prefix) {
        this.prefix = prefix;
    }

    public String prefix() {
        return prefix;
    }

    public String apply(String ulid) {
        return prefix + "_" + ulid;
    }
}
