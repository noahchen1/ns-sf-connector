package com.hamiltonjewelers.ns_sf_connector.controller;

import com.hamiltonjewelers.ns_sf_connector.model.SyncJob;
import com.hamiltonjewelers.ns_sf_connector.service.sync.discovery.customer.CustomerSyncCoordinator;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/sync")
public class SyncController {
    private final CustomerSyncCoordinator customerSyncCoordinator;

    public SyncController(CustomerSyncCoordinator customerSyncCoordinator) {
        this.customerSyncCoordinator = customerSyncCoordinator;
    }

    @PostMapping({"", "/customers"})
    public List<SyncJob> syncCustomers() {
        return customerSyncCoordinator.discoverAndEnqueue();
    }
}
