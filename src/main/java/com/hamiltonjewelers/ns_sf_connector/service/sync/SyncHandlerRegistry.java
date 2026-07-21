package com.hamiltonjewelers.ns_sf_connector.service.sync;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SyncHandlerRegistry {
    private final List<SyncHandler> handlers;

    public SyncHandlerRegistry(List<SyncHandler> handlers) {
        this.handlers = List.copyOf(handlers);
    }

    public SyncHandler forRoute(SyncRoute route) {
        return handlers.stream()
                .filter(handler -> handler.supports(route))
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException("Unsupported sync route: " + route));
    }
}
