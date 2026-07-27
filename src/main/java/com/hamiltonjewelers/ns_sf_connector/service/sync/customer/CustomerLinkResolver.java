package com.hamiltonjewelers.ns_sf_connector.service.sync.customer;

import com.hamiltonjewelers.ns_sf_connector.model.SyncJob;
import com.hamiltonjewelers.ns_sf_connector.service.sync.job.SyncRoute;
import com.hamiltonjewelers.ns_sf_connector.service.sync.job.SyncSystem;
import org.springframework.stereotype.Component;

@Component
public class CustomerLinkResolver {
    public int resolveNetsuiteCustomerId(SyncJob job, SyncRoute route) {
        String candidate;
        if (route.sourceSystem() == SyncSystem.NETSUITE || route.sourceSystem() == SyncSystem.SYSTEM) {
            candidate = job.getSourceRecordId();
        } else if (route.targetSystem() == SyncSystem.NETSUITE) {
            candidate = job.getTargetRecordId();
        } else {
            throw new IllegalArgumentException("Route does not identify a NetSuite customer: " + route);
        }

        if (candidate == null || candidate.isBlank()) {
            throw new IllegalArgumentException("Cannot resolve NetSuite customer ID for sync job " + job.getId());
        }
        try {
            return Integer.parseInt(candidate);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid NetSuite customer ID: " + candidate, e);
        }
    }
}
