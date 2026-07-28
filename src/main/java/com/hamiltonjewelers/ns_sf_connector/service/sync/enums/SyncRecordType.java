package com.hamiltonjewelers.ns_sf_connector.service.sync.enums;

public enum SyncRecordType {
    CUSTOMER;

    public static SyncRecordType from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Sync record type must not be blank");
        }
        return valueOf(value.trim().toUpperCase().replace(' ', '_'));
    }
}
