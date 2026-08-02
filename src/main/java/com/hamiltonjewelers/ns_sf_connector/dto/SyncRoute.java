package com.hamiltonjewelers.ns_sf_connector.dto;

import com.hamiltonjewelers.ns_sf_connector.enums.SyncOperation;
import com.hamiltonjewelers.ns_sf_connector.enums.SyncRecordType;
import com.hamiltonjewelers.ns_sf_connector.enums.SyncSystem;
import com.hamiltonjewelers.ns_sf_connector.model.SyncJob;

public record SyncRoute(
        SyncSystem sourceSystem,
        SyncSystem targetSystem,
        SyncRecordType recordType,
        SyncOperation operation
) {
    public static SyncRoute from(SyncJob job) {
        if (job == null) {
            throw new IllegalArgumentException("job must not be null");
        }
        return new SyncRoute(
                SyncSystem.from(job.getSourceSystem()),
                SyncSystem.from(job.getTargetSystem()),
                SyncRecordType.from(job.getRecordType()),
                SyncOperation.from(job.getOperation())
        );
    }

    public boolean is(SyncSystem source, SyncSystem target, SyncOperation operation) {
        return sourceSystem == source && targetSystem == target && this.operation == operation;
    }
}
