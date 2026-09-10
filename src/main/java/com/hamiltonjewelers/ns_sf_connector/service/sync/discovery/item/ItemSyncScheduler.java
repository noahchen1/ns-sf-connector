package com.hamiltonjewelers.ns_sf_connector.service.sync.discovery.item;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.sync.item.enabled", havingValue = "true", matchIfMissing = true)
public class ItemSyncScheduler {
    private final ItemSyncCoordinator coordinator;

    public ItemSyncScheduler(ItemSyncCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    @Scheduled(fixedDelayString = "${app.sync.item.poll-delay-ms:10000}")
    public void scheduleItemSync() { coordinator.discoverAndEnqueue(); }
}
