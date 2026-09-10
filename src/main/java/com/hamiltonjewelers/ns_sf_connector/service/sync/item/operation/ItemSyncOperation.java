package com.hamiltonjewelers.ns_sf_connector.service.sync.item.operation;

import com.hamiltonjewelers.ns_sf_connector.dto.ItemSyncContext;
import com.hamiltonjewelers.ns_sf_connector.dto.SyncRoute;
import com.hamiltonjewelers.ns_sf_connector.model.SyncJob;

public interface ItemSyncOperation {
    boolean supports(SyncRoute route);

    void execute(SyncJob job, ItemSyncContext context);
}
