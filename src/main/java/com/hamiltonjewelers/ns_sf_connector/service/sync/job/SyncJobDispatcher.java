package com.hamiltonjewelers.ns_sf_connector.service.sync.job;

import com.hamiltonjewelers.ns_sf_connector.model.SyncJob;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SyncJobDispatcher {
    private final List<SyncHandler> handlers;

    public SyncJobDispatcher(List<SyncHandler> handlers) {
        this.handlers = List.copyOf(handlers);
    }

    public void dispatch(SyncJob job) {
        SyncRoute route = SyncRoute.from(job);
        handlers.stream()
                .filter(handler -> handler.supports(route.recordType()))
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException("Unsupported sync route: " + route))
                .execute(job, route);
    }
}
