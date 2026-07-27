package com.hamiltonjewelers.ns_sf_connector.service.sync.job;

import com.hamiltonjewelers.ns_sf_connector.model.SyncJob;

public interface SyncHandler {
    boolean supports(SyncRecordType recordType);

    void execute(SyncJob job, SyncRoute route);
}
