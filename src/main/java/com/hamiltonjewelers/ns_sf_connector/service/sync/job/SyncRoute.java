package com.hamiltonjewelers.ns_sf_connector.service.sync.job;

import com.hamiltonjewelers.ns_sf_connector.model.SyncJob;
import com.hamiltonjewelers.ns_sf_connector.service.sync.enums.SyncOperation;
import com.hamiltonjewelers.ns_sf_connector.service.sync.enums.SyncRecordType;
import com.hamiltonjewelers.ns_sf_connector.service.sync.enums.SyncSystem;

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
