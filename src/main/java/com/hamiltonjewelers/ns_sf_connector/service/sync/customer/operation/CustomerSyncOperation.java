package com.hamiltonjewelers.ns_sf_connector.service.sync.customer.operation;

import com.hamiltonjewelers.ns_sf_connector.model.SyncJob;
import com.hamiltonjewelers.ns_sf_connector.service.sync.customer.CustomerSyncContext;
import com.hamiltonjewelers.ns_sf_connector.service.sync.job.SyncRoute;

public interface CustomerSyncOperation {
    boolean supports(SyncRoute route);

    void execute(SyncJob job, CustomerSyncContext context);
}
