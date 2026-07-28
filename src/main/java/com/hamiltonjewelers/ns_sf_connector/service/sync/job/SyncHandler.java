package com.hamiltonjewelers.ns_sf_connector.service.sync.job;

import com.hamiltonjewelers.ns_sf_connector.model.SyncJob;
import com.hamiltonjewelers.ns_sf_connector.service.sync.enums.SyncRecordType;

public interface SyncHandler {
    boolean supports(SyncRecordType recordType);

    void execute(SyncJob job, SyncRoute route);
}
