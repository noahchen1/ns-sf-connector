package com.hamiltonjewelers.ns_sf_connector.service.sync.discovery.customer;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.sync.customer.enabled", havingValue = "true", matchIfMissing = true)
public class CustomerSyncScheduler {
    private final CustomerSyncCoordinator coordinator;

    public CustomerSyncScheduler(CustomerSyncCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    @Scheduled(fixedDelayString = "${app.sync.customer.poll-delay-ms:10000}")
    public void scheduleCustomerSync() {
        coordinator.discoverAndEnqueue();
    }
}
