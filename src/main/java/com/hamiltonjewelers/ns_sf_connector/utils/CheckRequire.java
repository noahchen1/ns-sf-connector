package com.hamiltonjewelers.ns_sf_connector.utils;

public class CheckRequire {
    private CheckRequire() {}

    public static void requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing or blank config: " + name);
        }
    }
}
