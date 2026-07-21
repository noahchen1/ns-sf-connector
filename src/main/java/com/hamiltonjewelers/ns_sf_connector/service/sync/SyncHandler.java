package com.hamiltonjewelers.ns_sf_connector.service.sync;

import com.hamiltonjewelers.ns_sf_connector.model.SyncJob;

public interface SyncHandler {
    boolean supports(SyncRoute route);

    void execute(SyncJob job, SyncRoute route);
}
