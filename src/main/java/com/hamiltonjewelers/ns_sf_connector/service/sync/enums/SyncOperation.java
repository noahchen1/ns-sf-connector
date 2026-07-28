package com.hamiltonjewelers.ns_sf_connector.service.sync.enums;

public enum SyncOperation {
    INSERT,
    UPDATE,
    RECONCILE;

    public static SyncOperation from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Sync operation must not be blank");
        }
        return valueOf(value.trim().toUpperCase().replace(' ', '_'));
    }
}
