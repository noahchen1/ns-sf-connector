package com.hamiltonjewelers.ns_sf_connector.service.sync;

import com.hamiltonjewelers.ns_sf_connector.model.SyncJob;
import org.springframework.stereotype.Service;

@Service
public class SyncExecutor {
    private final SyncHandlerRegistry handlerRegistry;

    public SyncExecutor(SyncHandlerRegistry handlerRegistry) {
        this.handlerRegistry = handlerRegistry;
    }

    public void execute(SyncJob job) {
        if (job == null) {
            throw new IllegalArgumentException("job must not be null");
        }

        SyncRoute route = SyncRoute.from(job);
        handlerRegistry.forRoute(route).execute(job, route);
    }
}
