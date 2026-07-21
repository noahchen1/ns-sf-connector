package com.hamiltonjewelers.ns_sf_connector.service.sync;

import com.hamiltonjewelers.ns_sf_connector.model.SyncJob;

public record SyncRoute(String sourceSystem, String targetSystem, String recordType, String operation) {
    public static SyncRoute from(SyncJob job) {
        return new SyncRoute(
                normalize(job.getSourceSystem()),
                normalize(job.getTargetSystem()),
                normalize(job.getRecordType()),
                normalize(job.getOperation())
        );
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase().replace(' ', '_');
    }
}
