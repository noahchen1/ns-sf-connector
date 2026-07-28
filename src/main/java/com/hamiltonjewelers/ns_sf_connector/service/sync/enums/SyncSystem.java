package com.hamiltonjewelers.ns_sf_connector.service.sync.enums;

public enum SyncSystem {
    NETSUITE,
    SALESFORCE,
    SYSTEM;

    public static SyncSystem from(String value) {
        return valueOf(normalize(value));
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Sync system must not be blank");
        }
        return value.trim().toUpperCase().replace(' ', '_');
    }
}
