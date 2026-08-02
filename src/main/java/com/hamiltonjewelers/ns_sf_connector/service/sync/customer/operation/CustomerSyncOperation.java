package com.hamiltonjewelers.ns_sf_connector.service.sync.customer.operation;

import com.hamiltonjewelers.ns_sf_connector.model.SyncJob;
import com.hamiltonjewelers.ns_sf_connector.dto.CustomerSyncContext;
import com.hamiltonjewelers.ns_sf_connector.dto.SyncRoute;

public interface CustomerSyncOperation {
    boolean supports(SyncRoute route);

    void execute(SyncJob job, CustomerSyncContext context);
}
